package com.cronoslot

import android.content.*
import android.graphics.*
import android.media.ToneGenerator
import android.media.AudioManager
import android.os.*
import android.view.*
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import kotlin.math.abs

class CameraActivity:ComponentActivity(){
    private lateinit var preview:PreviewView
    private lateinit var overlay:LineOverlay
    private lateinit var info:TextView
    private lateinit var lapsView:TextView
    private val exec=Executors.newSingleThreadExecutor()
    private var previous:ByteArray?=null
    private var armed=true
    private var lastDetection=0L
    private var startedAt=0L
    private var lastLapAt=0L
    private val lapTimes=mutableListOf<Double>()
    private val tone=ToneGenerator(AudioManager.STREAM_NOTIFICATION,85)
    private var pilotId=0L;private var carId=0L;private var trackId=0L;private var remote="";private var calibration=false
    override fun onCreate(b:Bundle?){super.onCreate(b);pilotId=intent.getLongExtra("pilotId",0);carId=intent.getLongExtra("carId",0);trackId=intent.getLongExtra("trackId",0);remote=intent.getStringExtra("remote")?:"";calibration=intent.getBooleanExtra("calibrationOnly",false);build();start()}
    private fun build(){val root=FrameLayout(this);preview=PreviewView(this);root.addView(preview,FrameLayout.LayoutParams(-1,-1));overlay=LineOverlay(this);root.addView(overlay,FrameLayout.LayoutParams(-1,-1));val panel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;padding=16;setBackgroundColor(0xB0000000.toInt())};info=TextView(this).apply{textColor=Color.WHITE;textSize=16f;text="CALIBRACIÓN: coloca la línea roja sobre el punto de paso"};lapsView=TextView(this).apply{textColor=Color.WHITE;textSize=22f;text="Vueltas: 0"};panel.addView(info);panel.addView(lapsView);if(calibration){panel.addView(Button(this).apply{text="GUARDAR CALIBRACIÓN";setOnClickListener{getPreferences(0).edit().putFloat("lineX",overlay.lineX).apply();finish()}})}else{panel.addView(Button(this).apply{text="ARMAR DETECCIÓN";setOnClickListener{armed=true;info.text="DETECCIÓN ACTIVA"}});panel.addView(Button(this).apply{text="🔴 TERMINAR CARRERA";setOnClickListener{confirmFinish()}})};val pp=FrameLayout.LayoutParams(-1,-2);pp.gravity=Gravity.TOP;root.addView(panel,pp);setContentView(root);overlay.lineX=getPreferences(0).getFloat("lineX",.5f);if(!calibration)info.text="Pasa el coche por la línea roja"} 
    private fun start(){val f=ProcessCameraProvider.getInstance(this);f.addListener({val p=f.get();val pr=Preview.Builder().build().also{it.surfaceProvider=preview.surfaceProvider};val an=ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();an.setAnalyzer(exec){analyze(it)};p.unbindAll();p.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,pr,an)},ContextCompat.getMainExecutor(this))}
    private fun analyze(img:ImageProxy){try{val buf=img.planes[0].buffer;val data=ByteArray(buf.remaining());buf.get(data);val w=img.width;val h=img.height;val x=(overlay.lineX*w).toInt().coerceIn(10,w-11);var score=0.0;var count=0;previous?.let{old->if(old.size==data.size){for(y in (h*.25).toInt() until (h*.75).toInt() step 4)for(xx in x-10..x+10 step 3){val i=y*w+xx;if(i>=0&&i<data.size){score+=abs((data[i].toInt() and 255)-(old[i].toInt() and 255));count++}}}};previous=data;val s=if(count==0)0.0 else score/count;val now=SystemClock.elapsedRealtime();if(!calibration&&armed&&s>22&&now-lastDetection>250){lastDetection=now;armed=false;runOnUiThread{if(lastLapAt>0){val sec=(now-lastLapAt)/1000.0;lapTimes.add(sec);lapsView.text="Vuelta ${lapTimes.size}: %.3f s".format(sec);tone.startTone(ToneGenerator.TONE_PROP_BEEP,120)}else{startedAt=now;info.text="Primera detección: cronómetro iniciado"};lastLapAt=now}}else if(!armed&&s<9)armed=true}catch(_:Exception){}finally{img.close()}}
    private fun confirmFinish(){if(lapTimes.isEmpty()){finish();return};AlertDialog.Builder(this).setTitle("¿Realmente quieres terminar?").setMessage("Se guardarán todos los tiempos registrados.").setNegativeButton("CANCELAR",null).setPositiveButton("TERMINAR"){_,_->askNotes()}.show()}
    private fun askNotes(){val input=EditText(this);input.hint="Notas de la sesión (opcional)";AlertDialog.Builder(this).setTitle("Notas de la sesión").setView(input).setNegativeButton("OMITIR"){_,_->save("")}.setPositiveButton("GUARDAR SESIÓN"){_,_->save(input.text.toString())}.show()}
    private fun save(notes:String){val track=Db(this).tracks().find{it.id==trackId};if(track==null){finish();return};val best=lapTimes.minOrNull()?:0.0;val avg=lapTimes.average();val distance=track.length*lapTimes.size;val d=Db(this);val sid=d.insertSession(pilotId,carId,trackId,remote,startedAt,notes,lapTimes.size,best,avg,distance);lapTimes.forEachIndexed{i,v->d.insertLap(sid,i+1,v)};if(best>0)Toast.makeText(this,"Sesión guardada · mejor %.3f s".format(best),Toast.LENGTH_LONG).show();finish()}
    override fun onDestroy(){exec.shutdown();tone.release();super.onDestroy()}
    class LineOverlay(c:Context):View(c){var lineX=.5f;private val p=Paint(1).apply{color=Color.RED;strokeWidth=7f};private val h=Paint(1).apply{color=Color.WHITE};override fun onDraw(c:Canvas){val x=width*lineX;c.drawLine(x,0f,x,height.toFloat(),p);c.drawCircle(x,height*.5f,18f,h)};override fun onTouchEvent(e:MotionEvent):Boolean{if(e.action==MotionEvent.ACTION_DOWN||e.action==MotionEvent.ACTION_MOVE){lineX=(e.x/width).coerceIn(.03f,.97f);invalidate();return true};return true}}
}
