package com.cronoslot
import android.content.Context
import android.graphics.*
import android.media.*
import android.os.*
import android.view.*
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import kotlin.math.abs

class CameraActivity:ComponentActivity(){
 private lateinit var pv:PreviewView;private lateinit var line:LineView;private lateinit var info:TextView;private lateinit var list:TextView
 private val ex=Executors.newSingleThreadExecutor();private var prev:ByteArray?=null;private var armed=true;private var last=0L;private var lapAt=0L;private val laps=mutableListOf<Double>();private val tone=ToneGenerator(AudioManager.STREAM_NOTIFICATION,90)
 private val cooldown=300L
 override fun onCreate(b:Bundle?){super.onCreate(b);ui();camera()}
 private fun ui(){val root=FrameLayout(this);pv=PreviewView(this);root.addView(pv,FrameLayout.LayoutParams(-1,-1));line=LineView(this);root.addView(line,FrameLayout.LayoutParams(-1,-1));val p=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(12,12,12,12);setBackgroundColor(0xB0000000.toInt())};info=TextView(this).apply{text="Mueve la línea roja hasta la meta";setTextColor(Color.WHITE);textSize=16f};list=TextView(this).apply{text="Vueltas: 0";setTextColor(Color.WHITE);textSize=22f};p.addView(info);p.addView(list);p.addView(Button(this).apply{text="ARMAR DETECCIÓN";setOnClickListener{armed=true;info.text="DETECCIÓN ACTIVA"}});p.addView(Button(this).apply{text="🔴 TERMINAR CARRERA";setOnClickListener{finishRace()}});val pp=FrameLayout.LayoutParams(-1,-2);pp.gravity=Gravity.TOP;root.addView(p,pp);setContentView(root)}
 private fun camera(){val f=ProcessCameraProvider.getInstance(this);f.addListener({val c=f.get();val pr=Preview.Builder().build().also{it.surfaceProvider=pv.surfaceProvider};val an=ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();an.setAnalyzer(ex){a(it)};c.unbindAll();c.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,pr,an)},ContextCompat.getMainExecutor(this))}
 private fun a(img:ImageProxy){try{val b=img.planes[0].buffer;val d=ByteArray(b.remaining());b.get(d);val w=img.width;val h=img.height;val x=(line.xpos*w).toInt().coerceIn(12,w-13);var score=0.0;var n=0;prev?.let{o->if(o.size==d.size){for(y in (h*.25).toInt() until (h*.75).toInt() step 5)for(xx in x-10..x+10 step 3){val i=y*w+xx;if(i<d.size){score+=abs((d[i].toInt()and 255)-(o[i].toInt()and 255));n++}}}};prev=d;val s=if(n==0)0.0 else score/n;val now=SystemClock.elapsedRealtime();if(armed&&s>22&&now-last>cooldown){last=now;armed=false;runOnUiThread{if(lapAt>0){val sec=(now-lapAt)/1000.0;laps.add(sec);tone.startTone(ToneGenerator.TONE_PROP_BEEP,120);list.text="Vuelta ${laps.size}: %.3f s".format(sec)}else info.text="Primera detección — cronómetro iniciado";lapAt=now}}else if(!armed&&s<9)armed=true}catch(_:Exception){}finally{img.close()}}
 private fun finishRace(){AlertDialog.Builder(this).setTitle("¿Realmente quieres terminar?").setMessage("Se guardarán los tiempos registrados.").setNegativeButton("CANCELAR",null).setPositiveButton("TERMINAR"){_,_->notes()}.show()}
 private fun notes(){val e=EditText(this);e.hint="Notas de la sesión (opcional)";AlertDialog.Builder(this).setTitle("Notas de la sesión").setView(e).setNegativeButton("OMITIR"){_,_->finish()}.setPositiveButton("GUARDAR SESIÓN"){_,_->Toast.makeText(this,"Sesión preparada con ${laps.size} vueltas.",Toast.LENGTH_LONG).show();finish()}.show()}
 override fun onDestroy(){ex.shutdown();tone.release();super.onDestroy()}
 class LineView(c:Context):View(c){var xpos=.5f;private val p=Paint(1).apply{color=Color.RED;strokeWidth=7f};override fun onDraw(c:Canvas){val x=width*xpos;c.drawLine(x,0f,x,height.toFloat(),p)};override fun onTouchEvent(e:MotionEvent):Boolean{if(e.action==0||e.action==2){xpos=(e.x/width).coerceIn(.03f,.97f);invalidate();return true};return true}}
}
