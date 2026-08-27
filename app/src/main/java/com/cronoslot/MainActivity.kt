package com.cronoslot
import android.Manifest
import android.app.*
import android.content.*
import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity:ComponentActivity(){
 private lateinit var menu:LinearLayout
 private val cam=registerForActivityResult(ActivityResultContracts.RequestPermission()){ if(it) startActivity(Intent(this,CameraActivity::class.java)) }
 override fun onCreate(b:Bundle?){super.onCreate(b);setContentView(R.layout.activity_main);menu=findViewById(R.id.menu);build()}
 private fun button(t:String, f:()->Unit)=Button(this).apply{text=t;textSize=18f;minimumHeight=58;setOnClickListener{f()}}
 private fun build(){
  listOf("🏁  CARRERA" to ::career,"📋  REGISTROS" to ::records,"🏆  RÉCORDS" to ::records,"📊  ESTADÍSTICAS" to ::stats,"💾  DATOS" to ::data,"🎯  CALIBRACIÓN" to ::calibration,"📤  EXPORTAR" to ::exportData).forEach{(t,f)->menu.addView(button(t,f))}
 }
 private fun career(){startActivity(Intent(this,CameraActivity::class.java).apply{putExtra("race",true)})}
 private fun records(){AlertDialog.Builder(this).setTitle("RÉCORDS").setMessage("Aquí se mostrarán los récords por pista y por piloto+coche cuando existan sesiones.").setPositiveButton("OK",null).show()}
 private fun stats(){AlertDialog.Builder(this).setTitle("ESTADÍSTICAS").setMessage("Kilómetros, vueltas, velocidad media y estadísticas por piloto/coche/pista.").setPositiveButton("OK",null).show()}
 private fun data(){AlertDialog.Builder(this).setTitle("DATOS").setItems(arrayOf("Pilotos","Coches","Circuitos")){_,w->AlertDialog.Builder(this).setTitle("Datos").setMessage("Gestión de ${arrayOf("pilotos","coches","circuitos")[w]}; esta base queda preparada para la siguiente iteración.").setPositiveButton("OK",null).show()}.show()}
 private fun calibration(){if(checkSelfPermission(Manifest.permission.CAMERA)==0)startActivity(Intent(this,CameraActivity::class.java).putExtra("calibration",true))else cam.launch(Manifest.permission.CAMERA)}
 private fun exportData(){AlertDialog.Builder(this).setTitle("EXPORTAR").setMessage("La exportación a Excel/Google Drive se integrará sobre esta misma base.").setPositiveButton("OK",null).show()}
}
