package com.cronoslot;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends ComponentActivity {
    private Db db;
    private LinearLayout root;

    private final ActivityResultLauncher<String> cameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startActivity(new Intent(this, CameraActivity.class).putExtra("calibrationOnly", true));
                else toast("Se necesita permiso de cámara.");
            });

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        db = new Db(this);
        showHome();
    }

    private LinearLayout page(String name) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 70, 20, 20);
        root.setBackgroundColor(Color.rgb(245,247,249));
        TextView h = new TextView(this);
        h.setText(name);
        h.setTextSize(30);
        h.setTextColor(Color.rgb(25,28,32));
        h.setGravity(Gravity.CENTER);
        h.setPadding(0, 0, 0, 24);
        root.addView(h);
        setContentView(root);
        return root;
    }

    private Button btn(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(17);
        b.setAllCaps(false);
        b.setMinHeight(64);
        b.setOnClickListener(l);
        return b;
    }

    private TextView section(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(15);
        v.setTextColor(Color.DKGRAY);
        v.setPadding(6, 12, 6, 4);
        return v;
    }

    private EditText field(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(17);
        e.setMinHeight(58);
        e.setPadding(12, 10, 12, 10);
        return e;
    }

    private TextView info(String s) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(17);
        v.setTextColor(Color.rgb(40,45,50));
        v.setPadding(14, 14, 14, 14);
        return v;
    }

    private void back(LinearLayout p, Runnable target) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 16, 0, 0);
        p.addView(btn("← Volver", v -> target.run()), lp);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    private void showHome() {
        LinearLayout p = page("CronoSlot");
        TextView sub = info("Cronometraje de Slot");
        sub.setGravity(Gravity.CENTER);
        p.addView(sub);

        String[] names = {"🏁  Carrera","📋  Registros","🏆  Récords","📊  Estadísticas","💾  Datos","🎯  Calibración","📤  Exportar"};
        Runnable[] go = {this::career,this::records,this::recordsMenu,this::stats,this::dataMenu,this::calibration,this::export};
        for(int i=0;i<names.length;i++){
            final int n=i;
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
            lp.setMargins(0,6,0,6);
            p.addView(btn(names[i],v->go[n].run()),lp);
        }
    }

    private void dataMenu() {
        LinearLayout p=page("Datos");
        p.addView(btn("👤  Pilotos",v->pilots()));
        p.addView(btn("🚗  Coches",v->cars()));
        p.addView(btn("🏁  Circuitos",v->tracks()));
        back(p,this::showHome);
    }

    private void pilots() {
        LinearLayout p=page("Pilotos");
        List<Pilot> xs=db.pilots();
        if(xs.isEmpty()) p.addView(info("No hay pilotos. Añade el primero."));
        for(Pilot x:xs){
            LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(12,10,12,10);
            card.addView(info(x.label() + "\nMando: " + (x.remotes.isEmpty()?"—":x.remotes)));
            LinearLayout actions=new LinearLayout(this);
            actions.addView(btn("✏️ Editar",v->pilotForm(x)));
            actions.addView(btn("🗑️ Eliminar",v->confirmDelete("piloto",x.label(),()->{db.deletePilot(x.id);pilots();})));
            card.addView(actions); p.addView(card);
        }
        p.addView(btn("＋ Añadir piloto",v->pilotForm(null)));
        back(p,()->dataMenu());
    }

    private void pilotForm(Pilot existing) {
        LinearLayout p=page(existing==null?"Nuevo piloto":"Editar piloto");
        EditText n=field("Nombre *"), s=field("Apellidos"), r=field("Mando");
        if(existing!=null){n.setText(existing.name);s.setText(existing.surname);r.setText(existing.remotes);}
        p.addView(n);p.addView(s);p.addView(r);

        p.addView(btn("📷 Foto",v->toast("La selección de foto se añadirá en el siguiente ajuste de permisos/galería.")));
        p.addView(btn(existing==null?"Guardar piloto":"Guardar cambios",v->{
            if(n.getText().toString().trim().isEmpty()){n.setError("El nombre es obligatorio");return;}
            if(existing==null) db.addPilot(n.getText().toString().trim(),s.getText().toString().trim(),r.getText().toString().trim());
            else db.updatePilot(existing.id,n.getText().toString().trim(),s.getText().toString().trim(),r.getText().toString().trim());
            pilots();
        }));
        back(p,()->pilots());
    }

    private void cars() {
        LinearLayout p=page("Coches");
        List<Car> xs=db.cars();
        if(xs.isEmpty()) p.addView(info("No hay coches. Añade el primero."));
        for(Car x:xs){
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
            card.addView(info(x.name+"\n"+x.label()));
            LinearLayout actions=new LinearLayout(this);
            actions.addView(btn("✏️ Editar",v->carForm(x)));
            actions.addView(btn("🗑️ Eliminar",v->confirmDelete("coche",x.name,()->{db.deleteCar(x.id);cars();})));
            card.addView(actions);p.addView(card);
        }
        p.addView(btn("＋ Añadir coche",v->carForm(null)));
        back(p,()->dataMenu());
    }

    private void carForm(Car existing) {
        LinearLayout p=page(existing==null?"Nuevo coche":"Editar coche");
        EditText[] e={field("Nombre *"),field("Marca"),field("Modelo"),field("Chasis"),field("Neumáticos delanteros"),field("Neumáticos traseros"),field("Trencilla"),field("Notas")};
        for(EditText x:e)p.addView(x);
        if(existing!=null){e[0].setText(existing.name);e[1].setText(existing.brand);e[2].setText(existing.model);e[3].setText(existing.chassis);e[4].setText(existing.frontTyre);e[5].setText(existing.rearTyre);e[6].setText(existing.braid);e[7].setText(existing.notes);}
        p.addView(btn("📷 Foto",v->toast("La selección de foto se añadirá en el siguiente ajuste de permisos/galería.")));
        p.addView(btn(existing==null?"Guardar coche":"Guardar cambios",v->{
            if(e[0].getText().toString().trim().isEmpty()){e[0].setError("El nombre es obligatorio");return;}
            if(existing==null) db.addCar(e[0].getText().toString().trim(),e[1].getText().toString().trim(),e[2].getText().toString().trim(),e[3].getText().toString().trim(),e[4].getText().toString().trim(),e[5].getText().toString().trim(),e[6].getText().toString().trim(),e[7].getText().toString().trim());
            else db.updateCar(existing.id,e[0].getText().toString().trim(),e[1].getText().toString().trim(),e[2].getText().toString().trim(),e[3].getText().toString().trim(),e[4].getText().toString().trim(),e[5].getText().toString().trim(),e[6].getText().toString().trim(),e[7].getText().toString().trim());
            cars();
        }));
        back(p,()->cars());
    }

    private void tracks() {
        LinearLayout p=page("Circuitos");
        List<Track> xs=db.tracks();
        if(xs.isEmpty())p.addView(info("No hay circuitos. Añade el primero."));
        for(Track x:xs){
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
            card.addView(info(x.name+"\nLongitud: "+(x.length>0?String.format(Locale.getDefault(),"%.2f m",x.length):"—")));
            LinearLayout actions=new LinearLayout(this);
            actions.addView(btn("✏️ Editar",v->trackForm(x)));
            actions.addView(btn("🗑️ Eliminar",v->confirmDelete("circuito",x.name,()->{db.deleteTrack(x.id);tracks();})));
            card.addView(actions);p.addView(card);
        }
        p.addView(btn("＋ Añadir circuito",v->trackForm(null)));
        back(p,()->dataMenu());
    }

    private void trackForm(Track existing) {
        LinearLayout p=page(existing==null?"Nuevo circuito":"Editar circuito");
        EditText n=field("Nombre *"),len=field("Longitud (metros, opcional)"),min=field("Tiempo mínimo de vuelta (segundos, opcional)"),notes=field("Notas");
        len.setInputType(2|8192);min.setInputType(2|8192);
        if(existing!=null){n.setText(existing.name);len.setText(existing.length>0?String.valueOf(existing.length):"");notes.setText(existing.notes);}
        p.addView(n);p.addView(len);p.addView(min);p.addView(notes);
        p.addView(btn("📷 Foto",v->toast("La selección de foto se añadirá en el siguiente ajuste de permisos/galería.")));
        p.addView(btn(existing==null?"Guardar circuito":"Guardar cambios",v->{
            if(n.getText().toString().trim().isEmpty()){n.setError("El nombre es obligatorio");return;}
            double l=parse(len.getText().toString());
            if(existing==null) db.addTrack(n.getText().toString().trim(),l,notes.getText().toString().trim());
            else db.updateTrack(existing.id,n.getText().toString().trim(),l,notes.getText().toString().trim());
            tracks();
        }));
        back(p,()->tracks());
    }

    private double parse(String s){try{return Double.parseDouble(s.replace(',','.'));}catch(Exception e){return 0;}}
    private void confirmDelete(String kind,String name,Runnable yes){
        new AlertDialog.Builder(this).setTitle("Eliminar "+kind+"?")
                .setMessage("¿Realmente quieres eliminar «"+name+"»?")
                .setNegativeButton("Cancelar",null).setPositiveButton("Eliminar",(d,w)->yes.run()).show();
    }

    private void career(){
        List<Pilot> ps=db.pilots();List<Car> cs=db.cars();List<Track> ts=db.tracks();
        if(ps.isEmpty()||cs.isEmpty()||ts.isEmpty()){toast("Crea al menos un piloto, un coche y un circuito en DATOS.");return;}
        LinearLayout p=page("Nueva carrera");
        final Pilot[] pilot={ps.get(0)};final Car[] car={cs.get(0)};final Track[] track={ts.get(0)};final String[] remote={pilot[0].remotes};
        if(ps.size()>1){
            Spinner sp=new Spinner(this);sp.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labelsPilots(ps)));
            p.addView(section("Piloto"));p.addView(sp);
            sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> a){}public void onItemSelected(AdapterView<?> a,View v,int pos,long id){pilot[0]=ps.get(pos);remote[0]=pilot[0].remotes;}});
        }else p.addView(info("Piloto: "+pilot[0].label()));
        if(!remote[0].isEmpty())p.addView(info("Mando: "+remote[0]));
        if(cs.size()>1){Spinner sp=new Spinner(this);sp.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labelsCars(cs)));p.addView(section("Coche"));p.addView(sp);sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> a){}public void onItemSelected(AdapterView<?> a,View v,int pos,long id){car[0]=cs.get(pos);}});}else p.addView(info("Coche: "+car[0].name));
        if(ts.size()>1){Spinner sp=new Spinner(this);sp.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labelsTracks(ts)));p.addView(section("Pista"));p.addView(sp);sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> a){}public void onItemSelected(AdapterView<?> a,View v,int pos,long id){track[0]=ts.get(pos);}});}else p.addView(info("Pista: "+track[0].name));
        p.addView(btn("🏁  COMENZAR CARRERA",v->{Intent i=new Intent(this,CameraActivity.class);i.putExtra("pilotId",pilot[0].id);i.putExtra("carId",car[0].id);i.putExtra("trackId",track[0].id);i.putExtra("remote",remote[0]);startActivity(i);}));
        back(p,this::showHome);
    }

    private String[] labelsPilots(List<Pilot> a){String[] r=new String[a.size()];for(int i=0;i<a.size();i++)r[i]=a.get(i).label();return r;}
    private String[] labelsCars(List<Car> a){String[] r=new String[a.size()];for(int i=0;i<a.size();i++)r[i]=a.get(i).label();return r;}
    private String[] labelsTracks(List<Track> a){String[] r=new String[a.size()];for(int i=0;i<a.size();i++)r[i]=a.get(i).name;return r;}

    private void records(){
        LinearLayout p=page("Registros");
        List<Session> xs=db.sessions();
        if(xs.isEmpty())p.addView(info("No hay sesiones registradas."));
        for(Session s:xs)p.addView(info("#"+s.id+" · "+s.laps+" vueltas · mejor "+fmt(s.best)+" s · "+fmt(s.distance)+" m"));
        back(p,this::showHome);
    }

    private void recordsMenu(){
        LinearLayout p=page("Récords");
        List<Track> ts=db.tracks();List<Pilot> ps=db.pilots();
        if(ts.isEmpty()){p.addView(info("Crea un circuito primero."));back(p,this::showHome);return;}
        Spinner t=new Spinner(this);t.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labelsTracks(ts)));
        p.addView(section("Circuito"));p.addView(t);
        p.addView(btn("🏆 Récords absolutos",v->showRecords(ts.get(t.getSelectedItemPosition()).id,null,"Récords absolutos")));
        if(!ps.isEmpty()){
            Spinner pi=new Spinner(this);pi.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labelsPilots(ps)));
            p.addView(section("Piloto"));p.addView(pi);
            p.addView(btn("👤 Récords por piloto",v->showRecords(ts.get(t.getSelectedItemPosition()).id,ps.get(pi.getSelectedItemPosition()).id,"Récords por piloto")));
        }
        p.addView(btn("🎯 Récord exacto piloto + coche + circuito",v->exactRecordPage()));
        p.addView(btn("🔥 Últimos 20 mejores tiempos",v->top20Page()));
        back(p,this::showHome);
    }

    private void showRecords(long trackId,Long pilotId,String title){
        LinearLayout p=page(title);
        List<Object[]> rows=db.recordRows(trackId,pilotId);List<Pilot> ps=db.pilots();List<Car> cs=db.cars();
        if(rows.isEmpty())p.addView(info("Sin tiempos registrados."));
        for(int i=0;i<rows.size();i++){Object[] r=rows.get(i);Pilot pi=findPilot(ps,(Long)r[0]);Car c=findCar(cs,(Long)r[1]);p.addView(info((i+1)+". "+(pi==null?"Piloto":pi.label())+" · "+(c==null?"Coche":c.label())+" · "+fmt((Double)r[2])+" s"));}
        back(p,()->recordsMenu());
    }

    private void exactRecordPage(){
        List<Track> ts=db.tracks();List<Pilot> ps=db.pilots();List<Car> cs=db.cars();
        LinearLayout p=page("Récord por piloto + coche");
        if(ts.isEmpty()||ps.isEmpty()||cs.isEmpty()){p.addView(info("Necesitas al menos un circuito, piloto y coche."));back(p,this::recordsMenu);return;}
        Spinner tr=new Spinner(this);tr.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labelsTracks(ts)));
        Spinner pi=new Spinner(this);pi.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labelsPilots(ps)));
        Spinner ca=new Spinner(this);ca.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labelsCars(cs)));
        p.addView(section("Circuito"));p.addView(tr);p.addView(section("Piloto"));p.addView(pi);p.addView(section("Coche"));p.addView(ca);
        p.addView(btn("Ver mejor tiempo",v->{Double best=db.bestCombo(ts.get(tr.getSelectedItemPosition()).id,ps.get(pi.getSelectedItemPosition()).id,cs.get(ca.getSelectedItemPosition()).id);p.addView(info("Mejor tiempo: "+(best==null?"—":fmt(best)+" s")));}));
        back(p,this::recordsMenu);
    }

    private void top20Page(){
        LinearLayout p=page("Últimos 20 mejores tiempos");
        List<Session> ss=db.sessions();List<Pilot> ps=db.pilots();List<Car> cs=db.cars();List<Track> ts=db.tracks();
        class R{double s;Session x;R(double a,Session b){s=a;x=b;}}
        List<R> rows=new ArrayList<>();
        for(Session s:ss)for(Lap l:db.laps(s.id))rows.add(new R(l.seconds,s));
        rows.sort(Comparator.comparingDouble(a->a.s));
        int n=Math.min(20,rows.size());
        for(int i=0;i<n;i++){R r=rows.get(i);Pilot pi=findPilot(ps,r.x.pilotId);Car c=findCar(cs,r.x.carId);Track t=findTrack(ts,r.x.trackId);p.addView(info((i+1)+". "+fmt(r.s)+" s · "+(pi==null?"":pi.label())+" · "+(c==null?"":c.name)+" · "+(t==null?"":t.name)));}
        if(n==0)p.addView(info("Todavía no hay vueltas."));
        back(p,this::recordsMenu);
    }

    private void stats(){
        LinearLayout p=page("Estadísticas");
        double dist=0;double best=0;int laps=0;for(Session s:db.sessions()){dist+=s.distance;laps+=s.laps;if(s.best>0&&(best==0||s.best<best))best=s.best;}
        p.addView(info("Sesiones: "+db.sessions().size()+"\nVueltas: "+laps+"\nDistancia total: "+fmt(dist/1000)+" km\nMejor vuelta: "+(best==0?"—":fmt(best)+" s")));
        back(p,this::showHome);
    }

    private void calibration(){
        List<Track> ts=db.tracks();
        if(ts.isEmpty()){toast("Crea un circuito primero.");return;}
        LinearLayout p=page("Calibración");
        Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labelsTracks(ts)));
        p.addView(section("Circuito"));p.addView(s);
        p.addView(btn("🎯 Abrir cámara / calibración",v->{Intent i=new Intent(this,CameraActivity.class);i.putExtra("calibrationOnly",true);i.putExtra("trackId",ts.get(s.getSelectedItemPosition()).id);startActivity(i);}));
        back(p,this::showHome);
    }

    private void export(){
        LinearLayout p=page("Exportar");
        p.addView(info("Genera un Excel con sesiones y vueltas, ordenado por circuito. Puedes guardarlo en Google Drive desde el selector de Android."));
        p.addView(btn("📤 Generar Excel",v->toast("Exportación Excel: se integrará en la siguiente compilación final.")));
        back(p,this::showHome);
    }

    private Pilot findPilot(List<Pilot> a,long id){for(Pilot x:a)if(x.id==id)return x;return null;}
    private Car findCar(List<Car> a,long id){for(Car x:a)if(x.id==id)return x;return null;}
    private Track findTrack(List<Track> a,long id){for(Track x:a)if(x.id==id)return x;return null;}
    private String fmt(double x){return String.format(Locale.getDefault(),"%.3f",x);}
}
