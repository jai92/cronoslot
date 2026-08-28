package com.cronoslot;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends ComponentActivity {
    private Db db;
    private LinearLayout root;

    private int photoTarget = 0; // 1 pilot, 2 car, 3 track
    private long photoTargetId = -1L;
    private Uri pendingPhotoUri;

    private final androidx.activity.result.ActivityResultLauncher<String> photoPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    String saved = copyPhotoToInternalStorage(uri, photoTarget, photoTargetId);
                    if (saved != null) {
                        if (photoTarget == 1) db.updatePilotPhoto(photoTargetId, saved);
                        else if (photoTarget == 2) db.updateCarPhoto(photoTargetId, saved);
                        else if (photoTarget == 3) db.updateTrackPhoto(photoTargetId, saved);
                        Toast.makeText(this, "Foto guardada.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "No se pudo guardar la foto.", Toast.LENGTH_LONG).show();
                    }
                }
            });

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
        root.setPadding(20, 96, 20, 20);
        root.setBackgroundColor(Color.rgb(245,247,249));
        TextView h = new TextView(this);
        h.setText(name);
        h.setTextSize(38);
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
        b.setTextSize(22);
        b.setAllCaps(false);
        b.setMinHeight(82);
        b.setPadding(22, 20, 22, 20);
        b.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        b.setOnClickListener(l);
        return b;
    }

    private TextView section(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(20);
        v.setTextColor(Color.DKGRAY);
        v.setPadding(8, 16, 8, 8);
        return v;
    }

    private EditText field(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(21);
        e.setMinHeight(58);
        e.setPadding(12, 10, 12, 10);
        return e;
    }

    private void styleCard(View v) {
        v.setBackgroundColor(Color.WHITE);
        if (v instanceof TextView) {
            ((TextView) v).setTextColor(Color.rgb(35, 40, 45));
        }
        v.setElevation(3f);
    }


    private ImageView photoView(String path, int sizeDp) {
        ImageView image = new ImageView(this);
        int px = (int) (sizeDp * getResources().getDisplayMetrics().density + 0.5f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(px, px);
        lp.setMargins(8, 8, 16, 8);
        image.setLayoutParams(lp);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (path != null && !path.trim().isEmpty()) {
            android.graphics.Bitmap bitmap = BitmapFactory.decodeFile(path);
            if (bitmap != null) {
                image.setImageBitmap(bitmap);
            } else {
                image.setImageResource(android.R.drawable.ic_menu_camera);
            }
        } else {
            image.setImageResource(android.R.drawable.ic_menu_camera);
        }
        return image;
    }

    private LinearLayout photoHeader(String path, String titleText, String subtitleText) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(8, 8, 8, 8);

        ImageView image = photoView(path, 76);
        header.addView(image);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView title = info(titleText);
        title.setTextSize(22);
        title.setPadding(0, 4, 0, 2);
        texts.addView(title);

        if (subtitleText != null && !subtitleText.isEmpty()) {
            TextView sub = info(subtitleText);
            sub.setTextSize(18);
            sub.setTextColor(Color.DKGRAY);
            sub.setPadding(0, 2, 0, 4);
            texts.addView(sub);
        }

        header.addView(texts, new LinearLayout.LayoutParams(0, -2, 1f));
        return header;
    }

    private TextView info(String s) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(21);
        v.setTextColor(Color.rgb(40,45,50));
        v.setPadding(18, 20, 18, 20);
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
            lp.setMargins(0,10,0,10);
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
            card.addView(photoHeader(x.photo, x.label(), "Mando: " + (x.remotes.isEmpty()?"—":x.remotes)));
            LinearLayout actions=new LinearLayout(this);
            actions.addView(btn("✏️ Editar",v->pilotForm(x)));
            actions.addView(btn("🗑️ Eliminar",v->confirmDelete("piloto",x.label(),()->{db.deletePilot(x.id);pilots();})));
            card.addView(actions); p.addView(card); p.addView(separator());
        }
        p.addView(btn("＋ Añadir piloto",v->pilotForm(null)));
        back(p,()->dataMenu());
    }

    private void pilotForm(Pilot existing) {
        LinearLayout p=page(existing==null?"Nuevo piloto":"Editar piloto");
        EditText n=field("Nombre *"), s=field("Apellidos"), r=field("Mando");
        if(existing!=null){n.setText(existing.name);s.setText(existing.surname);r.setText(existing.remotes);}
        p.addView(photoHeader(existing == null ? null : existing.photo, "Foto del piloto", existing == null ? "Añade la foto después de guardar" : "Imagen actual"));
        p.addView(n);p.addView(s);p.addView(r);

        p.addView(btn("📷 Añadir foto",v->{ if(existing==null){ Toast.makeText(this,"Guarda primero el piloto y después añade la foto desde Editar.",Toast.LENGTH_LONG).show(); } else { photoTarget=2; photoTargetId=existing.id; photoPicker.launch("image/*"); } }));
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
            card.addView(photoHeader(x.photo, x.name, x.label()));
            LinearLayout actions=new LinearLayout(this);
            actions.addView(btn("✏️ Editar",v->carForm(x)));
            actions.addView(btn("🗑️ Eliminar",v->confirmDelete("coche",x.name,()->{db.deleteCar(x.id);cars();})));
            card.addView(actions);p.addView(card);p.addView(separator());
        }
        p.addView(btn("＋ Añadir coche",v->carForm(null)));
        back(p,()->dataMenu());
    }

    private void carForm(Car existing) {
        LinearLayout p=page(existing==null?"Nuevo coche":"Editar coche");
        EditText[] e={field("Nombre *"),field("Marca"),field("Modelo"),field("Chasis"),field("Neumáticos delanteros"),field("Neumáticos traseros"),field("Trencilla"),field("Notas")};
        if(existing!=null) p.addView(photoHeader(existing.photo, existing.name, "Foto del coche"));
        for(EditText x:e)p.addView(x);
        if(existing!=null){e[0].setText(existing.name);e[1].setText(existing.brand);e[2].setText(existing.model);e[3].setText(existing.chassis);e[4].setText(existing.frontTyre);e[5].setText(existing.rearTyre);e[6].setText(existing.braid);e[7].setText(existing.notes);}
        p.addView(btn("📷 Añadir foto",v->{ if(existing==null){ Toast.makeText(this,"Guarda primero el piloto y después añade la foto desde Editar.",Toast.LENGTH_LONG).show(); } else { photoTarget=3; photoTargetId=existing.id; photoPicker.launch("image/*"); } }));
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
            card.addView(photoHeader(x.photo, x.name, "Longitud: " + (x.length>0?String.format(Locale.getDefault(),"%.2f m",x.length):"—") + " · Mínimo: " + (x.minLap>0?fmt(x.minLap)+" s":"—")));
            LinearLayout actions=new LinearLayout(this);
            actions.addView(btn("✏️ Editar",v->trackForm(x)));
            actions.addView(btn("🗑️ Eliminar",v->confirmDelete("circuito",x.name,()->{db.deleteTrack(x.id);tracks();})));
            card.addView(actions);p.addView(card);p.addView(separator());
        }
        p.addView(btn("＋ Añadir circuito",v->trackForm(null)));
        back(p,()->dataMenu());
    }

    private void trackForm(Track existing) {
        LinearLayout p=page(existing==null?"Nuevo circuito":"Editar circuito");
        EditText n=field("Nombre *"),len=field("Longitud (metros, opcional)"),min=field("Tiempo mínimo de vuelta (segundos, opcional)"),notes=field("Notas");
        len.setInputType(2|8192);min.setInputType(2|8192);
        if(existing!=null){n.setText(existing.name);len.setText(existing.length>0?String.valueOf(existing.length):"");min.setText(existing.minLap>0?String.valueOf(existing.minLap):"");notes.setText(existing.notes);}
        if(existing!=null) p.addView(photoHeader(existing.photo, existing.name, "Foto del circuito"));
        p.addView(n);p.addView(len);p.addView(min);p.addView(notes);
        p.addView(btn("📷 Añadir foto",v->{ if(existing==null){ Toast.makeText(this,"Guarda primero el piloto y después añade la foto desde Editar.",Toast.LENGTH_LONG).show(); } else { photoTarget=1; photoTargetId=existing.id; photoPicker.launch("image/*"); } }));
        p.addView(btn(existing==null?"Guardar circuito":"Guardar cambios",v->{
            if(n.getText().toString().trim().isEmpty()){n.setError("El nombre es obligatorio");return;}
            double l=parse(len.getText().toString());
            double m=parse(min.getText().toString());
            if(existing==null) db.addTrack(n.getText().toString().trim(),l,m,notes.getText().toString().trim());
            else db.updateTrack(existing.id,n.getText().toString().trim(),l,m,notes.getText().toString().trim());
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
        final Pilot[] pilot={ps.get(0)};
final Car[] car={cs.get(0)};final Track[] track={ts.get(0)};final String[] remote={pilot[0].remotes};
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
        List<Track> ts=db.tracks();
        List<Pilot> ps=db.pilots();

        p.addView(btn("🏆  Récords absolutos",v->absoluteRecordsPage()));
        p.addView(btn("👤  Récords por piloto",v->pilotRecordsPage()));
        p.addView(btn("🎯  Récords por piloto + coche",v->exactRecordPage()));

        if(ts.isEmpty()){
            p.addView(info("Crea al menos un circuito para consultar récords."));
        }

        back(p,this::showHome);
    }

    private void absoluteRecordsPage(){
        List<Track> ts=db.tracks();
        LinearLayout p=page("Récords absolutos");

        if(ts.isEmpty()){
            p.addView(info("No hay circuitos registrados."));
            back(p,this::recordsMenu);
            return;
        }

        Spinner track=new Spinner(this);
        track.setAdapter(new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                labelsTracks(ts)
        ));
        p.addView(section("Circuito"));
        p.addView(track);

        LinearLayout results=new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        p.addView(results,new LinearLayout.LayoutParams(-1,0,1));

        Runnable refresh=()->{
            results.removeAllViews();
            List<Object[]> rows=db.top20Absolute(ts.get(track.getSelectedItemPosition()).id);
            List<Pilot> pilots=db.pilots();
            List<Car> cars=db.cars();
            if(rows.isEmpty()){
                results.addView(info("Sin tiempos registrados en este circuito."));
                return;
            }
            for(int i=0;i<rows.size();i++){
                Object[] r=rows.get(i);
                Pilot pi=findPilot(pilots,(Long)r[1]);
                Car c=findCar(cars,(Long)r[2]);
                results.addView(info(
                        (i+1)+".  "+fmt((Double)r[0])+" s  ·  "+
                        (pi==null?"":pi.label())+"  ·  "+
                        (c==null?"":c.label())+"  ·  "+
                        fmtDate((Long)r[3])
                ));
                if(i<rows.size()-1) results.addView(separator());
            }
        };

        track.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> a){}
            public void onItemSelected(AdapterView<?> a,View v,int pos,long id){refresh.run();}
        });
        refresh.run();
        back(p,this::recordsMenu);
    }

    private void pilotRecordsPage(){
        List<Track> ts=db.tracks();
        List<Pilot> ps=db.pilots();
        LinearLayout p=page("Récords por piloto");

        if(ts.isEmpty() || ps.isEmpty()){
            p.addView(info("Necesitas al menos un piloto y un circuito."));
            back(p,this::recordsMenu);
            return;
        }

        Spinner track=null;
        if(ts.size()>1){
            track=new Spinner(this);
            track.setAdapter(new ArrayAdapter<String>(
                    this,android.R.layout.simple_spinner_dropdown_item,labelsTracks(ts)
            ));
            p.addView(section("Circuito"));
            p.addView(track);
        }

        Spinner pilot=null;
        if(ps.size()>1){
            pilot=new Spinner(this);
            pilot.setAdapter(new ArrayAdapter<String>(
                    this,android.R.layout.simple_spinner_dropdown_item,labelsPilots(ps)
            ));
            p.addView(section("Piloto"));
            p.addView(pilot);
        }

        final Spinner trackSpinner=track;
        final Spinner pilotSpinner=pilot;
        LinearLayout results=new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        p.addView(results,new LinearLayout.LayoutParams(-1,0,1));

        Runnable refresh=()->{
            results.removeAllViews();
            Track tr=trackSpinner==null?ts.get(0):ts.get(trackSpinner.getSelectedItemPosition());
            Pilot pi=pilotSpinner==null?ps.get(0):ps.get(pilotSpinner.getSelectedItemPosition());
            List<Object[]> rows=db.top20PilotTrack(tr.id,pi.id);
            List<Car> cars=db.cars();
            if(rows.isEmpty()){
                results.addView(info("Sin tiempos registrados para este piloto en esta pista."));
                return;
            }
            for(int i=0;i<rows.size();i++){
                Object[] r=rows.get(i);
                Car c=findCar(cars,(Long)r[1]);
                results.addView(info(
                        (i+1)+".  "+fmt((Double)r[0])+" s  ·  "+
                        (c==null?"":c.label())+"  ·  "+fmtDate((Long)r[2])
                ));
                if(i<rows.size()-1) results.addView(separator());
            }
        };

        if(trackSpinner!=null){
            trackSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
                public void onNothingSelected(AdapterView<?> a){}
                public void onItemSelected(AdapterView<?> a,View v,int pos,long id){refresh.run();}
            });
        }
        if(pilotSpinner!=null){
            pilotSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
                public void onNothingSelected(AdapterView<?> a){}
                public void onItemSelected(AdapterView<?> a,View v,int pos,long id){refresh.run();}
            });
        }

        refresh.run();
        back(p,this::recordsMenu);
    }

    private void exactRecordPage(){
        List<Track> ts=db.tracks();
        List<Pilot> ps=db.pilots();
        List<Car> cs=db.cars();
        LinearLayout p=page("Récords por piloto + coche");

        if(ts.isEmpty() || ps.isEmpty() || cs.isEmpty()){
            p.addView(info("Necesitas al menos un piloto, coche y circuito."));
            back(p,this::recordsMenu);
            return;
        }

        Spinner track=null,pilot=null,car=null;

        if(ts.size()>1){
            track=new Spinner(this);
            track.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labelsTracks(ts)));
            p.addView(section("Circuito"));p.addView(track);
        }
        if(ps.size()>1){
            pilot=new Spinner(this);
            pilot.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labelsPilots(ps)));
            p.addView(section("Piloto"));p.addView(pilot);
        }
        if(cs.size()>1){
            car=new Spinner(this);
            car.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labelsCars(cs)));
            p.addView(section("Coche"));p.addView(car);
        }

        final Spinner trackSpinner=track;
        final Spinner pilotSpinner=pilot;
        final Spinner carSpinner=car;

        LinearLayout results=new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        p.addView(results,new LinearLayout.LayoutParams(-1,0,1));

        Runnable refresh=()->{
            results.removeAllViews();
            Track tr=trackSpinner==null?ts.get(0):ts.get(trackSpinner.getSelectedItemPosition());
            Pilot pi=pilotSpinner==null?ps.get(0):ps.get(pilotSpinner.getSelectedItemPosition());
            Car ca=carSpinner==null?cs.get(0):cs.get(carSpinner.getSelectedItemPosition());
            List<Object[]> rows=db.top20Exact(tr.id,pi.id,ca.id);
            if(rows.isEmpty()){
                results.addView(info("Sin tiempos registrados para esta combinación."));
                return;
            }
            for(int i=0;i<rows.size();i++){
                Object[] r=rows.get(i);
                results.addView(info((i+1)+".  "+fmt((Double)r[0])+" s  ·  "+fmtDate((Long)r[1])));
                if(i<rows.size()-1) results.addView(separator());
            }
        };

        if(trackSpinner!=null) trackSpinner.setOnItemSelectedListener(listener(refresh));
        if(pilotSpinner!=null) pilotSpinner.setOnItemSelectedListener(listener(refresh));
        if(carSpinner!=null) carSpinner.setOnItemSelectedListener(listener(refresh));

        refresh.run();
        back(p,this::recordsMenu);
    }

    private AdapterView.OnItemSelectedListener listener(Runnable r){
        return new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> a){}
            public void onItemSelected(AdapterView<?> a,View v,int pos,long id){r.run();}
        };
    }

    private Pilot findPilot(List<Pilot> a,long id){for(Pilot x:a)if(x.id==id)return x;return null;}
    private Car findCar(List<Car> a,long id){for(Car x:a)if(x.id==id)return x;return null;}
    private Track findTrack(List<Track> a,long id){for(Track x:a)if(x.id==id)return x;return null;}
    private String fmt(double x){return String.format(Locale.getDefault(),"%.3f",x);}
    private String fmtDate(long millis){return new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(new Date(millis));}

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
        p.addView(btn("📤 Generar Excel",v->startExcelExport()));
        back(p,this::showHome);
    }


    private void startExcelExport() {
        try {
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            i.putExtra(Intent.EXTRA_TITLE, "CronoSlot.xlsx");
            exportLauncher.launch(i);
        } catch (Exception e) {
            toast("No se pudo abrir el selector de archivo.");
        }
    }

    private final ActivityResultLauncher<Intent> exportLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null
                                && result.getData().getData() != null) {
                            try {
                                WorkbookBuilder.write(this, result.getData().getData(), db);
                                toast("Excel generado correctamente.");
                            } catch (Exception e) {
                                toast("Error al generar Excel: " + e.getMessage());
                            }
                        }
                    });


    private String copyPhotoToInternalStorage(Uri uri, int target, long id) {
        try {
            String folder = target == 1 ? "pilots" : (target == 2 ? "cars" : "tracks");
            java.io.File dir = new java.io.File(getFilesDir(), folder);
            if (!dir.exists() && !dir.mkdirs()) return null;

            java.io.File outFile = new java.io.File(dir, "photo_" + id + ".jpg");
            android.graphics.Bitmap bitmap =
                    android.provider.MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

            java.io.FileOutputStream fos = new java.io.FileOutputStream(outFile);
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, fos);
            fos.flush();
            fos.close();
            bitmap.recycle();

            return outFile.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    private Pilot findPilot(List<Pilot> a,long id){for(Pilot x:a)if(x.id==id)return x;return null;}
    private Car findCar(List<Car> a,long id){for(Car x:a)if(x.id==id)return x;return null;}
    private Track findTrack(List<Track> a,long id){for(Track x:a)if(x.id==id)return x;return null;}
    private String fmt(double x){return String.format(Locale.getDefault(),"%.3f",x);}
}
