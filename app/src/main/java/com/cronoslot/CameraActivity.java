package com.cronoslot;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.media.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.activity.ComponentActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import java.util.*;
import java.util.concurrent.*;


public class CameraActivity extends ComponentActivity {
 PreviewView preview; LineOverlay overlay; TextView info,lapsView; Db db; ExecutorService exec=Executors.newSingleThreadExecutor();
 byte[] previous; boolean armed=true,calibrationOnly=false; long lastDetection=0,lastLapAt=0,raceStart=0; List<Double> lapTimes=new ArrayList<>();
 long pilotId,carId,trackId; String remote=""; ToneGenerator tone; android.content.SharedPreferences prefs;

 @Override public void onCreate(Bundle b){super.onCreate(b);db=new Db(this);pilotId=getIntent().getLongExtra("pilotId",0);carId=getIntent().getLongExtra("carId",0);trackId=getIntent().getLongExtra("trackId",0);remote=getIntent().getStringExtra("remote");calibrationOnly=getIntent().getBooleanExtra("calibrationOnly",false);prefs=getSharedPreferences("calibration",0);build();startCamera();}
 void build(){FrameLayout root=new FrameLayout(this);preview=new PreviewView(this);root.addView(preview,new FrameLayout.LayoutParams(-1,-1));overlay=new LineOverlay(this);root.addView(overlay,new FrameLayout.LayoutParams(-1,-1));overlay.lineX=prefs.getFloat("lineX_"+trackId,.5f);
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(14,14,14,14);panel.setBackgroundColor(0xBB000000);info=new TextView(this);info.setTextColor(Color.WHITE);info.setTextSize(17);lapsView=new TextView(this);lapsView.setTextColor(Color.WHITE);lapsView.setTextSize(23);lapsView.setText("Vueltas: 0");panel.addView(info);panel.addView(lapsView);
  if(calibrationOnly){info.setText("Mueve la línea roja hasta el punto de paso.");panel.addView(btn("GUARDAR CALIBRACIÓN",v->{prefs.edit().putFloat("lineX_"+trackId,overlay.lineX).apply();Toast.makeText(this,"Calibración guardada.",Toast.LENGTH_SHORT).show();finish();}));}
  else{info.setText("Pasa el coche por la línea roja.");panel.addView(btn("ARMAR DETECCIÓN",v->{armed=true;info.setText("DETECCIÓN ACTIVA");}));panel.addView(btn("🔴 TERMINAR CARRERA",v->confirmFinish()));}
  FrameLayout.LayoutParams pp=new FrameLayout.LayoutParams(-1,-2);pp.gravity=Gravity.TOP;root.addView(panel,pp);setContentView(root);tone=new ToneGenerator(AudioManager.STREAM_NOTIFICATION,95);
 }
 Button btn(String t,View.OnClickListener l){Button b=new Button(this);b.setText(t);b.setOnClickListener(l);return b;}
 void startCamera(){ProcessCameraProvider.getInstance(this).addListener(()->{try{ProcessCameraProvider p=ProcessCameraProvider.getInstance(this).get();Preview pr=new Preview.Builder().build();pr.setSurfaceProvider(preview.getSurfaceProvider());ImageAnalysis an=new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888).build();an.setAnalyzer(exec,this::analyze);p.unbindAll();p.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,pr,an);}catch(Exception e){runOnUiThread(()->info.setText("Error cámara: "+e.getMessage()));}},ContextCompat.getMainExecutor(this));}
 void analyze(ImageProxy img){try{ByteBufferView b=new ByteBufferView(img.planes[0].getBuffer());byte[] data=b.data();int w=img.getWidth(),h=img.getHeight();int x=(int)(overlay.lineX*w);double score=0;int n=0;if(previous!=null&&previous.length==data.length){for(int y=(int)(h*.2);y<(int)(h*.8);y+=4)for(int xx=Math.max(0,x-14);xx<Math.min(w,x+15);xx+=3){int i=y*w+xx;if(i>=0&&i<data.length){score+=Math.abs((data[i]&255)-(previous[i]&255));n++;}}}previous=data;double s=n==0?0:score/n;long now=SystemClock.elapsedRealtime();if(!calibrationOnly&&armed&&s>22&&now-lastDetection>250){lastDetection=now;armed=false;runOnUiThread(()->{if(lastLapAt==0){raceStart=now;info.setText("CRONÓMETRO INICIADO");}else{double sec=(now-lastLapAt)/1000.0;lapTimes.add(sec);lapsView.setText("Vuelta "+lapTimes.size()+" · "+String.format(Locale.getDefault(),"%.3f s",sec));recordTone(sec);}lastLapAt=now;});}else if(!armed&&s<9)armed=true;}catch(Exception ignored){}finally{img.close();}}
 void recordTone(double sec){Double bt=db.bestTrack(trackId),bc=db.bestCombo(trackId,pilotId,carId);boolean tr=bt==null||sec<bt,co=bc==null||sec<bc;if(co){tone.startTone(ToneGenerator.TONE_PROP_BEEP,140);if(tr){new Handler(Looper.getMainLooper()).postDelayed(()->tone.startTone(ToneGenerator.TONE_PROP_BEEP,140),190);info.setText("🏆 DOBLE RÉCORD");}else info.setText("🏎️ RÉCORD DE COCHE");}else if(tr){tone.startTone(ToneGenerator.TONE_PROP_BEEP,140);info.setText("🏆 RÉCORD DE PISTA");}}
 void confirmFinish(){new AlertDialog.Builder(this).setTitle("¿Realmente quieres terminar la sesión?").setMessage("Se guardarán todos los tiempos registrados.").setNegativeButton("NO, CONTINUAR",null).setPositiveButton("SÍ, TERMINAR",(d,w)->askNotes()).show();}
 void askNotes(){EditText e=new EditText(this);e.setHint("Notas de la sesión (opcional)");new AlertDialog.Builder(this).setTitle("Notas de la sesión").setMessage("Puedes anotar neumáticos u otras condiciones de esta prueba.").setView(e).setNeutralButton("OMITIR",(d,w)->save("")) .setPositiveButton("GUARDAR SESIÓN",(d,w)->save(e.getText().toString())).show();}
 void save(String notes){if(lapTimes.isEmpty()){Toast.makeText(this,"No hay vueltas registradas.",Toast.LENGTH_SHORT).show();finish();return;}db.addSession(pilotId,carId,trackId,remote,raceStart,notes,lapTimes);Toast.makeText(this,"Sesión guardada.",Toast.LENGTH_LONG).show();finish();}
 @Override protected void onDestroy(){if(exec!=null)exec.shutdown();if(tone!=null)tone.release();super.onDestroy();}
 static class ByteBufferView{java.nio.ByteBuffer buf;ByteBufferView(java.nio.ByteBuffer b){buf=b;}byte[] data(){byte[] d=new byte[buf.remaining()];buf.get(d);return d;}}
 static class LineOverlay extends View{float lineX=.5f;Paint p=new Paint(1);Paint h=new Paint(1);LineOverlay(Context c){super(c);p.setColor(Color.RED);p.setStrokeWidth(7);h.setColor(Color.WHITE);}protected void onDraw(Canvas c){super.onDraw(c);float x=getWidth()*lineX;c.drawLine(x,0,x,getHeight(),p);c.drawCircle(x,getHeight()/2f,18,h);}public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN||e.getAction()==MotionEvent.ACTION_MOVE){lineX=Math.max(.03f,Math.min(.97f,e.getX()/getWidth()));invalidate();return true;}return true;}}
}
