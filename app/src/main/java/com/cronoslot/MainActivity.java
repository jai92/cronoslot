package com.cronoslot;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.activity.ComponentActivity;
import androidx.activity.result.contract.ActivityResultContracts;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends ComponentActivity {
    private Db db;
    private LinearLayout root;

    private final androidx.activity.result.ActivityResultLauncher<String> cameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startActivity(new Intent(this, CameraActivity.class).putExtra("calibrationOnly", true));
                else Toast.makeText(this, "Se necesita permiso de cámara.", Toast.LENGTH_LONG).show();
            });

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        db = new Db(this);
        showHome();
    }

    private TextView title(String s) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextColor(Color.WHITE); v.setTextSize(28f);
        v.setGravity(Gravity.CENTER); v.setPadding(0, 10, 0, 22);
        return v;
    }

    private Button action(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text); b.setTextSize(17f); b.setAllCaps(false);
        b.setMinHeight(64); b.setPadding(16, 10, 16, 10);
        b.setOnClickListener(l);
        return b;
    }

    private LinearLayout page(String titleText) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18, 18, 18, 18);
        root.setBackgroundColor(Color.rgb(18, 18, 18));

        TextView t = title(titleText);
        root.addView(t);
        setContentView(root);
        return root;
    }

    private void backButton(LinearLayout page) {
        Button back = action("← Volver", v -> showHome());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 18, 0, 8);
        page.addView(back, lp);
    }

    private void showHome() {
        LinearLayout p = page("CronoSlot");
        TextView sub = new TextView(this);
        sub.setText("Cronometraje de Slot");
        sub.setTextColor(Color.LTGRAY); sub.setTextSize(16f);
        sub.setGravity(Gravity.CENTER); sub.setPadding(0,0,0,18);
        p.addView(sub);

        String[][] items = {
            {"🏁 Carrera", "Selecciona piloto, mando, coche y pista"},
            {"📋 Registros", "Historial completo de sesiones"},
            {"🏆 Récords", "Récords absolutos y por piloto"},
            {"📊 Estadísticas", "Kilómetros, vueltas y mejores tiempos"},
            {"💾 Datos", "Pilotos, coches y circuitos"},
            {"🎯 Calibración", "Configura la cámara y la línea de meta"},
            {"📤 Exportar", "Genera un Excel para guardarlo"}
        };
        Runnable[] actions = {
            this::career, this::records, this::recordsMenu, this::stats,
            this::dataMenu, this::calibration, this::export
        };
        for (int i=0;i<items.length;i++) {
            Button b = action(items[i][0] + "\n" + items[i][1], v -> actions[i].run());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, 5, 0, 5);
            p.addView(b, lp);
        }
    }

    private void dataMenu() {
        LinearLayout p = page("Datos");
        p.addView(action("👤 Pilotos", v -> pilots()));
        p.addView(action("🚗 Coches", v -> cars()));
        p.addView(action("🏁 Circuitos", v -> tracks()));
        backButton(p);
    }

    private void pilots() {
        LinearLayout p = page("Pilotos");
        List<Pilot> list = db.pilots();
        if (list.isEmpty()) p.addView(empty("No hay pilotos todavía."));
        for (Pilot x : list) {
            TextView row = row(x.name + " " + x.surname + "\nMandos: " + (x.remotes == null ? "" : x.remotes));
            p.addView(row);
        }
        p.addView(action("＋ Añadir piloto", v -> pilotForm()));
        backButton(p);
    }

    private void pilotForm() {
        LinearLayout p = page("Nuevo piloto");
        EditText name = field("Nombre *");
        EditText surname = field("Apellidos");
        EditText remotes = field("Mandos (separados por coma)");
        p.addView(name); p.addView(surname); p.addView(remotes);
        p.addView(action("Guardar piloto", v -> {
            if (name.getText().toString().trim().isEmpty()) {
                name.setError("El nombre es obligatorio"); return;
            }
            db.addPilot(name.getText().toString().trim(), surname.getText().toString().trim(), remotes.getText().toString().trim());
            pilots();
        }));
        backButton(p);
    }

    private void cars() {
        LinearLayout p = page("Coches");
        List<Car> list = db.cars();
        if (list.isEmpty()) p.addView(empty("No hay coches todavía."));
        for (Car x : list) p.addView(row(x.name + "\n" + x.brand + " " + x.model));
        p.addView(action("＋ Añadir coche", v -> carForm()));
        backButton(p);
    }

    private void carForm() {
        LinearLayout p = page("Nuevo coche");
        EditText name = field("Nombre *");
        EditText brand = field("Marca");
        EditText model = field("Modelo");
        EditText chassis = field("Chasis");
        EditText front = field("Neumáticos delanteros");
        EditText rear = field("Neumáticos traseros");
        EditText braid = field("Trencilla");
        EditText notes = field("Notas");
        for (EditText e : new EditText[]{name,brand,model,chassis,front,rear,braid,notes}) p.addView(e);
        p.addView(action("Guardar coche", v -> {
            if (name.getText().toString().trim().isEmpty()) { name.setError("El nombre es obligatorio"); return; }
            db.addCar(name.getText().toString().trim(), brand.getText().toString().trim(), model.getText().toString().trim(),
                    chassis.getText().toString().trim(), front.getText().toString().trim(), rear.getText().toString().trim(),
                    braid.getText().toString().trim(), notes.getText().toString().trim());
            cars();
        }));
        backButton(p);
    }

    private void tracks() {
        LinearLayout p = page("Circuitos");
        List<Track> list = db.tracks();
        if (list.isEmpty()) p.addView(empty("No hay circuitos todavía."));
        for (Track x : list) p.addView(row(x.name + "\nLongitud: " + x.length + " m"));
        p.addView(action("＋ Añadir circuito", v -> trackForm()));
        backButton(p);
    }

    private void trackForm() {
        LinearLayout p = page("Nuevo circuito");
        EditText name = field("Nombre *");
        EditText len = field("Longitud (metros)");
        len.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText notes = field("Notas");
        p.addView(name); p.addView(len); p.addView(notes);
        p.addView(action("Guardar circuito", v -> {
            if (name.getText().toString().trim().isEmpty()) { name.setError("El nombre es obligatorio"); return; }
            double l = parseDouble(len.getText().toString());
            db.addTrack(name.getText().toString().trim(), l, notes.getText().toString().trim());
            tracks();
        }));
        backButton(p);
    }

    private EditText field(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint); e.setTextSize(17f);
        e.setTextColor(Color.WHITE); e.setHintTextColor(Color.GRAY);
        e.setPadding(12, 16, 12, 16);
        return e;
    }

    private TextView row(String s) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextColor(Color.WHITE); v.setTextSize(17f);
        v.setPadding(14, 18, 14, 18);
        return v;
    }

    private TextView empty(String s) {
        TextView v = row(s); v.setTextColor(Color.LTGRAY); return v;
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s.replace(',', '.')); } catch(Exception e) { return 0.0; }
    }

    private void career() {
        List<Pilot> ps = db.pilots();
        List<Car> cars = db.cars();
        List<Track> ts = db.tracks();

        if (ps.isEmpty() || cars.isEmpty() || ts.isEmpty()) {
            Toast.makeText(this, "Crea al menos un piloto, un coche y un circuito en DATOS.", Toast.LENGTH_LONG).show();
            return;
        }

        LinearLayout p = page("Nueva carrera");
        final Pilot[] pilot = { ps.get(0) };
        final Car[] car = { cars.get(0) };
        final Track[] track = { ts.get(0) };
        final String[] remote = { pilot[0].remotes == null ? "" : pilot[0].remotes };

        LinearLayout remoteBox = new LinearLayout(this);
        remoteBox.setOrientation(LinearLayout.VERTICAL);

        if (ps.size() > 1) {
            Spinner sp = new Spinner(this);
            sp.setAdapter(new ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    labelsPilots(ps)
            ));
            p.addView(label("Piloto"));
            p.addView(sp);

            sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onNothingSelected(AdapterView<?> a) {}

                public void onItemSelected(AdapterView<?> a, View v, int pos, long id) {
                    pilot[0] = ps.get(pos);
                    remote[0] = pilot[0].remotes == null ? "" : pilot[0].remotes;
                    remoteBox.removeAllViews();
                    if (!remote[0].trim().isEmpty()) {
                        remoteBox.addView(label("Mando"));
                        remoteBox.addView(row(remote[0].trim()));
                    }
                    updateSummary();
                }

                private void updateSummary() {}
            });
        }

        if (!pilot[0].remotes.trim().isEmpty()) {
            remoteBox.addView(label("Mando"));
            remoteBox.addView(row(pilot[0].remotes.trim()));
        }
        p.addView(remoteBox);

        if (cars.size() > 1) {
            Spinner sp = new Spinner(this);
            sp.setAdapter(new ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    labelsCars(cars)
            ));
            p.addView(label("Coche"));
            p.addView(sp);
            sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onNothingSelected(AdapterView<?> a) {}

                public void onItemSelected(AdapterView<?> a, View v, int pos, long id) {
                    car[0] = cars.get(pos);
                }
            });
        }

        if (ts.size() > 1) {
            Spinner sp = new Spinner(this);
            sp.setAdapter(new ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    labelsTracks(ts)
            ));
            p.addView(label("Pista"));
            p.addView(sp);
            sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onNothingSelected(AdapterView<?> a) {}

                public void onItemSelected(AdapterView<?> a, View v, int pos, long id) {
                    track[0] = ts.get(pos);
                }
            });
        }

        p.addView(row(
                "Piloto: " + pilot[0].label() +
                "\nMando: " + (remote[0].trim().isEmpty() ? "—" : remote[0].trim()) +
                "\nCoche: " + car[0].name +
                "\nPista: " + track[0].name
        ));

        p.addView(action("🏁  COMENZAR CARRERA", v -> {
            Intent i = new Intent(this, CameraActivity.class);
            i.putExtra("pilotId", pilot[0].id);
            i.putExtra("carId", car[0].id);
            i.putExtra("trackId", track[0].id);
            i.putExtra("remote", remote[0].trim());
            startActivity(i);
        }));

        backButton(p);
    }

    private void records() {
        List<Session> all=db.sessions();
        LinearLayout p=page("Registros");
        if(all.isEmpty()) p.addView(empty("No hay sesiones registradas."));
        else{
            p.addView(label("Las sesiones se muestran de más reciente a más antigua."));
            for(Session s:all) p.addView(row("Sesión #"+s.id+" · "+s.laps+" vueltas · mejor "+fmt(s.best)+" s"));
        }
        backButton(p);
    }

    private void recordsMenu(){
        List<Track> ts=db.tracks(), psTempTracks=ts;
        List<Pilot> ps=db.pilots();
        LinearLayout p=page("Récords");
        if(ts.isEmpty()){p.addView(empty("Crea un circuito primero."));backButton(p);return;}
        Spinner t=new Spinner(this);t.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labelsTracks(ts)));
        p.addView(label("Circuito"));p.addView(t);
        p.addView(action("🏆 Récords absolutos",v->showRecords(ts.get(t.getSelectedItemPosition()).id,null)));
        if(!ps.isEmpty()){
            Spinner pi=new Spinner(this);pi.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labelsPilots(ps)));
            p.addView(label("Piloto"));p.addView(pi);
            p.addView(action("👤 Récords por piloto",v->showRecords(ts.get(t.getSelectedItemPosition()).id,ps.get(pi.getSelectedItemPosition()).id)));
        }
        backButton(p);
    }

    private void showRecords(long trackId, Long pilotId){
        List<Object[]> rows=db.recordRows(trackId,pilotId);
        List<Pilot> ps=db.pilots(); List<Car> cs=db.cars();
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<rows.size();i++){
            Object[] r=rows.get(i); Pilot p=findPilot(ps,(long)r[0]); Car c=findCar(cs,(long)r[1]);
            sb.append(i+1).append(". ").append(p==null?"Piloto":p.name+" "+p.surname)
              .append(" · ").append(c==null?"Coche":c.name)
              .append(" · ").append(fmt((double)r[2])).append(" s\n");
        }
        new AlertDialog.Builder(this).setTitle("Clasificación").setMessage(sb.length()==0?"Sin tiempos registrados.":sb.toString()).setPositiveButton("OK",null).show();
    }

    private Pilot findPilot(List<Pilot> a,long id){for(Pilot x:a)if(x.id==id)return x;return null;}
    private Car findCar(List<Car> a,long id){for(Car x:a)if(x.id==id)return x;return null;}

    private void stats(){
        List<Session> s=db.sessions();
        double km=s.stream().mapToDouble(x->x.distance).sum()/1000.0;
        double best=s.stream().filter(x->x.best>0).mapToDouble(x->x.best).min().orElse(0);
        LinearLayout p=page("Estadísticas");
        p.addView(row("Sesiones: "+s.size()+"\nKilómetros: "+fmt(km)+" km\nMejor vuelta: "+(best==0?"—":fmt(best)+" s")));
        backButton(p);
    }

    private void calibration(){
        List<Track> ts=db.tracks();
        if(ts.isEmpty()){Toast.makeText(this,"Crea un circuito primero.",Toast.LENGTH_SHORT).show();return;}
        LinearLayout p=page("Calibración");
        Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labelsTracks(ts)));
        p.addView(label("Circuito"));p.addView(s);
        p.addView(action("🎯 Abrir calibración",v->{
            Intent i=new Intent(this,CameraActivity.class).putExtra("calibrationOnly",true).putExtra("trackId",ts.get(s.getSelectedItemPosition()).id);
            startActivity(i);
        }));
        backButton(p);
    }

    private void export(){
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        i.putExtra(Intent.EXTRA_TITLE,"CronoSlot.xlsx");
        exportResult.launch(i);
    }

    private final androidx.activity.result.ActivityResultLauncher<Intent> exportResult =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if(result.getResultCode()==RESULT_OK && result.getData()!=null && result.getData().getData()!=null){
                    Toast.makeText(this,"Archivo listo para guardar en la ubicación elegida.",Toast.LENGTH_LONG).show();
                }
            });

    private String fmt(double x){return String.format(Locale.getDefault(),"%.3f",x);}
}
