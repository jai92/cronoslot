package com.cronoslot

import android.graphics.*
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import kotlin.math.abs

class CameraActivity : ComponentActivity() {
    private lateinit var preview: PreviewView
    private lateinit var overlay: LineOverlay
    private lateinit var status: TextView
    private lateinit var laps: TextView
    private val executor = Executors.newSingleThreadExecutor()
    private var lastGray: ByteArray? = null
    private var armed = true
    private var lastLapNs = 0L
    private var lapStartNs = 0L
    private var lap = 0
    private val threshold = 22.0
    private val cooldownMs = 250L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this)
        preview = PreviewView(this)
        root.addView(preview, FrameLayout.LayoutParams(-1,-1))
        overlay = LineOverlay(this)
        root.addView(overlay, FrameLayout.LayoutParams(-1,-1))
        val panel = LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL; setPadding(20,20,20,20)
            setBackgroundColor(0xAA000000.toInt())
        }
        status = TextView(this).apply { text="CALIBRACIÓN: mueve la línea roja hasta la zona de paso"; setTextColor(Color.WHITE); textSize=16f }
        laps = TextView(this).apply { text="Vueltas: 0"; setTextColor(Color.WHITE); textSize=22f }
        val test = Button(this).apply { text="ARMAR DETECCIÓN" }
        val finish = Button(this).apply { text="TERMINAR"; setOnClickListener { finish() } }
        panel.addView(status); panel.addView(laps); panel.addView(test); panel.addView(finish)
        val pp=FrameLayout.LayoutParams(-1,-2); pp.gravity=Gravity.TOP
        root.addView(panel,pp)
        setContentView(root)
        test.setOnClickListener { armed=true; status.text="DETECCIÓN ACTIVA — pasa el coche por la línea roja" }
        startCamera()
    }

    private fun startCamera() {
        val future=ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider=future.get()
            val previewUse=Preview.Builder().build().also { it.surfaceProvider=preview.surfaceProvider }
            val analysis=ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888).build()
            analysis.setAnalyzer(executor) { image -> analyze(image) }
            provider.unbindAll()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, previewUse, analysis)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyze(image: ImageProxy) {
        try {
            val plane=image.planes[0]
            val w=image.width; val h=image.height
            val y=plane.buffer
            val data=ByteArray(y.remaining()); y.get(data)
            // ROI central: una banda alrededor de la calibratable virtual line.
            val xLine=(overlay.lineX*w).toInt().coerceIn(1,w-2)
            val half=12
            var motion=0.0; var count=0
            val prev=lastGray
            if(prev!=null && prev.size==data.size) {
                for (yy in (h*0.30).toInt() until (h*0.70).toInt() step 4) {
                    for (xx in (xLine-half).coerceAtLeast(0) until (xLine+half).coerceAtMost(w-1) step 4) {
                        val i=yy*w+xx
                        if(i<data.size) { motion += abs((data[i].toInt() and 255)-(prev[i].toInt() and 255)); count++ }
                    }
                }
            }
            lastGray=data
            val score=if(count==0) 0.0 else motion/count
            val now=SystemClock.elapsedRealtimeNanos()
            if(armed && score>threshold && (now-lastLapNs)/1_000_000>cooldownMs) {
                lastLapNs=now
                runOnUiThread {
                    if(lapStartNs!=0L) {
                        lap++
                        val ms=(now-lapStartNs)/1_000_000.0
                        laps.text="Vuelta $lap   %.3f s".format(ms/1000.0)
                    } else { status.text="Primer paso detectado — cronómetro iniciado" }
                    lapStartNs=now
                }
                armed=false
            } else if(!armed && score < threshold*0.45) armed=true
        } finally { image.close() }
    }

    override fun onDestroy() { executor.shutdown(); super.onDestroy() }

    class LineOverlay(ctx: android.content.Context): View(ctx) {
        var lineX=0.50f
        private val p=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.RED; strokeWidth=6f }
        private val handle=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.WHITE }
        override fun onDraw(c:Canvas) { super.onDraw(c); val x=width*lineX; c.drawLine(x,0f,x,height.toFloat(),p); c.drawCircle(x,height*0.5f,18f,handle) }
        override fun onTouchEvent(e:android.view.MotionEvent):Boolean {
            if(e.action==android.view.MotionEvent.ACTION_DOWN || e.action==android.view.MotionEvent.ACTION_MOVE) { lineX=(e.x/width).coerceIn(.05f,.95f); invalidate(); return true }
            return true
        }
    }
}
