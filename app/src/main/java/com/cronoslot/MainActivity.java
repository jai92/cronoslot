package com.cronoslot;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import java.io.*;
import java.text.*;
import java.util.*;
import org.apache.poi.xssf.usermodel.*;

public class MainActivity extends ComponentActivity {
    Db db; LinearLayout menu; Uri pendingUri; java.util.function.Consumer<String> photoConsumer;
    ActivityResultLauncher<Uri> takePhoto; ActivityResultLauncher<String> pickPhoto; ActivityResultLauncher<Intent> exportLauncher;

    @Override public void onCreate(Bundle b){super.onCreate(b);db=new Db(this);build();setupLaunchers();render();}
    @Override protected void onResume(){super.onResume();if(menu!=null)render();}
    void build(){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(18,18,18,18);
        TextView title=new TextView(this);title.setText("CronoSlot");title.setTextSize(34);title.setTextColor(Color.BLACK);title.setGravity(Gravity.CENTER);r.addView(title);
        TextView sub=new TextView(this);sub.setText("Cronometraje de Slot");sub.setGravity(Gravity.CENTER);sub.setPadding(0,0,0,14);r.addView(sub);
        ScrollView sv=new ScrollView(this);menu=new LinearLayout(this);menu.setOrientation(LinearLayout.VERTICAL);sv.addView(menu);r.addView(sv,new LinearLayout.LayoutParams(-1,0,1));setContentView(r);}
    void setupLaunchers(){
        takePhoto=registerForActivityResult(new ActivityResultContracts.TakePicture(),ok->{if(ok&&pendingUri!=null&&photoConsumer!=null)photoConsumer.accept(pendingUri.toString());});
        pickPhoto=registerForActivityResult(new ActivityResultContracts.GetContent(),uri->{if(uri!=null&&photoConsumer!=null)photoConsumer.accept(uri.toString());});
        exportLauncher=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),r->{if(r.getResultCode()==RESULT_OK&&r.getData()!=null&&r.getData().getData()!=null)writeExcel(r.getData().getData());});
    }
    Button btn(String s,View.OnClickListener l){Button b=new Button(this);b.setText(s);b.setTextSize(18);b.setMinHeight(60);b.setOnClickListener(l);return b;}
    void render(){menu.removeAllViews();menu.addView(btn("🏁  CARRERA",v->career()));menu.addView(btn("📋  REGISTROS",v->records()));menu.addView(btn("🏆  RÉCORDS",v->recordsMenu()));menu.addView(btn("📊  ESTADÍSTICAS",v->stats()));menu.addView(btn("💾  DATOS",v->dataMenu()));menu.addView(btn("🎯  CALIBRACIÓN",v->calibration()));menu.addView(btn("📤  EXPORTAR",v->export()));}
    LinearLayout shell(String title){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(18,18,18,18);TextView t=new TextView(this);t.setText(title);t.setTextSize(24);l.addView(t);return l;}
    void dialog(LinearLayout l){Dialog d=new Dialog(this);d.setContentView(l);Window w=d.getWindow();if(w!=null)w.setLayout(-1,-2);d.show();}
    ImageView image(String uri){ImageView iv=new ImageView(this);iv.setScaleType(ImageView.ScaleType.CENTER_CROP);iv.setLayoutParams(new LinearLayout.LayoutParams(130,130));if(uri!=null)iv.setImageURI(Uri.parse(uri));return iv;}
    void choosePhoto(java.util.function.Consumer<String> c){photoConsumer=c;new AlertDialog.Builder(this).setTitle("Foto").setItems(new String[]{"Cámara","Galería","Cancelar"},(d,w)->{if(w==0){File dir=new File(getFilesDir(),"images");dir.mkdirs();File f=new File(dir,"photo_"+System.currentTimeMillis()+".jpg");pendingUri=FileProvider.getUriForFile(this,"com.cronoslot.fileprovider",f);takePhoto.launch(pendingUri);}else if(w==1)pickPhoto.launch("image/*");else photoConsumer=null;}).show();}
    EditText field(String hint,String val){EditText e=new EditText(this);e.setHint(hint);if(val!=null)e.setText(val);return e;}
    void dataMenu(){LinearLayout l=shell("DATOS");l.addView(btn("👤 PILOTOS",v->pilots()));l.addView(btn("🚗 COCHES",v->cars()));l.addView(btn("🏁 CIRCUITOS",v->tracks()));dialog(l);}
    void pilots(){Dialog d=new Dialog(this);LinearLayout l=shell("PILOTOS");for(Pilot p:db.pilots()){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);if(p.photo!=null)row.addView(image(p.photo));TextView tx=new TextView(this);tx.setText(p.label()+"\nMandos: "+String.join(", ",p.remotes));tx.setTextSize(16);tx.setPadding(10,4,10,4);row.addView(tx,new LinearLayout.LayoutParams(0,-2,1));row.addView(btn("EDITAR",v->{d.dismiss();pilotForm(p);}));row.addView(btn("BORRAR",v->{confirm("¿Borrar este piloto?",()->{db.deletePilot(p.id);d.dismiss();pilots();});}));l.addView(row);}l.addView(btn("＋ AÑADIR PILOTO",v->{d.dismiss();pilotForm(null);}));d.setContentView(l);d.show();}
    void pilotForm(Pilot old){Dialog d=new Dialog(this);LinearLayout l=shell(old==null?"NUEVO PILOTO":"EDITAR PILOTO");ImageView iv=image(old==null?null:old.photo);l.addView(iv);l.addView(btn("📷 AÑADIR/CAMBIAR FOTO",v->choosePhoto(uri->{iv.setImageURI(Uri.parse(uri));iv.setTag(uri);})));EditText n=field("Nombre",old==null?null:old.name),s=field("Apellidos",old==null?null:old.surname),r=field("Mandos (uno por línea o coma)",old==null?null:String.join("\n",old.remotes));l.addView(n);l.addView(s);l.addView(r);l.addView(btn("GUARDAR",v->{if(n.getText().length()==0||s.getText().length()==0)return;List<String> rem=new ArrayList<>();for(String x:r.getText().toString().split("[,\\n]"))if(!x.trim().isEmpty())rem.add(x.trim());String photo=iv.getTag()==null?(old==null?null:old.photo):(String)iv.getTag();Pilot p=new Pilot(old==null?0:old.id,n.getText().toString(),s.getText().toString(),rem,photo);if(old==null)db.addPilot(p);else db.updatePilot(p);d.dismiss();pilots();}));d.setContentView(l);d.show();}
    void cars(){Dialog d=new Dialog(this);LinearLayout l=shell("COCHES");for(Car c:db.cars()){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);if(c.photo!=null)row.addView(image(c.photo));TextView tx=new TextView(this);tx.setText(c.label()+"\nChasis: "+c.chassis);tx.setTextSize(16);tx.setPadding(10,4,10,4);row.addView(tx,new LinearLayout.LayoutParams(0,-2,1));row.addView(btn("EDITAR",v->{d.dismiss();carForm(c);}));row.addView(btn("BORRAR",v->{confirm("¿Borrar este coche?",()->{db.deleteCar(c.id);d.dismiss();cars();});}));l.addView(row);}l.addView(btn("＋ AÑADIR COCHE",v->{d.dismiss();carForm(null);}));d.setContentView(l);d.show();}
    void carForm(Car old){Dialog d=new Dialog(this);LinearLayout l=shell(old==null?"NUEVO COCHE":"EDITAR COCHE");ImageView iv=image(old==null?null:old.photo);l.addView(iv);l.addView(btn("📷 AÑADIR/CAMBIAR FOTO",v->choosePhoto(uri->{iv.setImageURI(Uri.parse(uri));iv.setTag(uri);})));EditText[] e={field("Marca",old==null?null:old.brand),field("Modelo",old==null?null:old.model),field("Chasis",old==null?null:old.chassis),field("Neumáticos delanteros",old==null?null:old.frontTyre),field("Neumáticos traseros",old==null?null:old.rearTyre),field("Trencilla",old==null?null:old.braid),field("Notas",old==null?null:old.notes)};for(EditText x:e)l.addView(x);l.addView(btn("GUARDAR",v->{if(e[0].getText().length()==0||e[1].getText().length()==0)return;String photo=iv.getTag()==null?(old==null?null:old.photo):(String)iv.getTag();Car c=new Car(old==null?0:old.id,e[0].getText().toString(),e[1].getText().toString(),e[2].getText().toString(),e[3].getText().toString(),e[4].getText().toString(),e[5].getText().toString(),e[6].getText().toString(),photo);if(old==null)db.addCar(c);else db.updateCar(c);d.dismiss();cars();}));d.setContentView(l);d.show();}
    void tracks(){Dialog d=new Dialog(this);LinearLayout l=shell("CIRCUITOS");for(Track t:db.tracks()){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);if(t.photo!=null)row.addView(image(t.photo));TextView tx=new TextView(this);tx.setText(t.name+"\nLongitud: "+String.format(Locale.getDefault(),"%.2f m",t.length));tx.setTextSize(16);tx.setPadding(10,4,10,4);row.addView(tx,new LinearLayout.LayoutParams(0,-2,1));row.addView(btn("EDITAR",v->{d.dismiss();trackForm(t);}));row.addView(btn("BORRAR",v->{confirm("¿Borrar este circuito?",()->{db.deleteTrack(t.id);d.dismiss();tracks();});}));l.addView(row);}l.addView(btn("＋ AÑADIR CIRCUITO",v->{d.dismiss();trackForm(null);}));d.setContentView(l);d.show();}
    void trackForm(Track old){Dialog d=new Dialog(this);LinearLayout l=shell(old==null?"NUEVO CIRCUITO":"EDITAR CIRCUITO");ImageView iv=image(old==null?null:old.photo);l.addView(iv);l.addView(btn("📷 AÑADIR/CAMBIAR FOTO",v->choosePhoto(uri->{iv.setImageURI(Uri.parse(uri));iv.setTag(uri);})));EditText n=field("Nombre",old==null?null:old.name),len=field("Longitud en metros",old==null?null:String.valueOf(old.length)),notes=field("Notas",old==null?null:old.notes);len.setInputType(2);l.addView(n);l.addView(len);l.addView(notes);l.addView(btn("GUARDAR",v->{try{double m=Double.parseDouble(len.getText().toString().replace(',','.'));if(n.getText().length()==0||m<=0)return;String photo=iv.getTag()==null?(old==null?null:old.photo):(String)iv.getTag();Track t=new Track(old==null?0:old.id,n.getText().toString(),m,notes.getText().toString(),photo);if(old==null)db.addTrack(t);else db.updateTrack(t);d.dismiss();tracks();}catch(Exception ignored){}}));d.setContentView(l);d.show();}
    void career(){
        List<Pilot> ps=db.pilots();
        List<Car> cs=db.cars();
        List<Track> ts=db.tracks();
        if(ps.isEmpty()||cs.isEmpty()||ts.isEmpty()){
            Toast.makeText(this,"Crea primero piloto, coche y circuito en DATOS.",Toast.LENGTH_LONG).show();
            return;
        }
        Dialog d=new Dialog(this);
        LinearLayout l=shell("NUEVA CARRERA");
        final Pilot[] selectedPilot={ps.get(0)};
        final Car[] selectedCar={cs.get(0)};
        final Track[] selectedTrack={ts.get(0)};
        final String[] selectedRemote={""};
        final AutoCompleteTextView[] pilotField={null};
        final AutoCompleteTextView[] carField={null};
        final AutoCompleteTextView[] trackField={null};
        final Spinner remoteSpinner=new Spinner(this);
        final TextView remoteLabel=new TextView(this);

        if(ps.size()>1){
            l.addView(new TextView(this){{setText("Piloto");}});
            AutoCompleteTextView p=new AutoCompleteTextView(this);
            pilotField[0]=p;
            p.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line,labelsPilots(ps)));
            p.setThreshold(0);
            p.setText(ps.get(0).label(),false);
            l.addView(p);
        }

        remoteLabel.setText("Mando");
        List<String> initialRemotes=selectedPilot[0].remotes;
        if(initialRemotes.size()>1){
            l.addView(remoteLabel);
            l.addView(remoteSpinner);
            remoteSpinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,initialRemotes));
            selectedRemote[0]=initialRemotes.get(0);
        }

        if(cs.size()>1){
            l.addView(new TextView(this){{setText("Coche");}});
            AutoCompleteTextView c=new AutoCompleteTextView(this);
            carField[0]=c;
            c.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line,labelsCars(cs)));
            c.setThreshold(0);
            c.setText(cs.get(0).label(),false);
            l.addView(c);
        }

        if(ts.size()>1){
            l.addView(new TextView(this){{setText("Pista");}});
            AutoCompleteTextView t=new AutoCompleteTextView(this);
            trackField[0]=t;
            t.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line,labelsTracks(ts)));
            t.setThreshold(0);
            t.setText(ts.get(0).name,false);
            l.addView(t);
        }

        TextView chosen=new TextView(this);
        chosen.setTextSize(16);
        chosen.setPadding(0,18,0,10);
        Runnable refreshSummary=()->chosen.setText(
            "Piloto: "+selectedPilot[0].label()+
            "\nMando: "+(selectedRemote[0].isEmpty()?"—":selectedRemote[0])+            "\nCoche: "+selectedCar[0].label()+
            "\nPista: "+selectedTrack[0].name);

        Runnable refreshRemote=()->{
            List<String> rr=selectedPilot[0].remotes;
            if(rr.size()>1){
                remoteLabel.setText("Mando");
                remoteSpinner.setVisibility(View.VISIBLE);
                remoteSpinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,rr));
                selectedRemote[0]=rr.get(0);
                remoteSpinner.setSelection(0);
            } else {
                remoteSpinner.setVisibility(View.GONE);
                selectedRemote[0]=rr.isEmpty()?"":rr.get(0);
            }
            refreshSummary.run();
        };

        if(pilotField[0]!=null){
            final AutoCompleteTextView p=pilotField[0];
            p.setOnItemClickListener((a,v,pos,id)->{selectedPilot[0]=findPilot(ps,p.getText().toString());refreshRemote.run();});
            p.setOnFocusChangeListener((v,has)->{if(!has){selectedPilot[0]=findPilot(ps,p.getText().toString());refreshRemote.run();}});
        }
        remoteSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> a){}
            public void onItemSelected(AdapterView<?> a,View v,int pos,long id){
                List<String> rr=selectedPilot[0].remotes;
                if(!rr.isEmpty()){selectedRemote[0]=rr.get(Math.max(0,Math.min(pos,rr.size()-1)));refreshSummary.run();}
            }
        });
        if(carField[0]!=null){
            final AutoCompleteTextView c=carField[0];
            c.setOnItemClickListener((a,v,pos,id)->{selectedCar[0]=findCar(cs,c.getText().toString());refreshSummary.run();});
            c.setOnFocusChangeListener((v,has)->{if(!has){selectedCar[0]=findCar(cs,c.getText().toString());refreshSummary.run();}});
        }
        if(trackField[0]!=null){
            final AutoCompleteTextView t=trackField[0];
            t.setOnItemClickListener((a,v,pos,id)->{selectedTrack[0]=findTrack(ts,t.getText().toString());refreshSummary.run();});
            t.setOnFocusChangeListener((v,has)->{if(!has){selectedTrack[0]=findTrack(ts,t.getText().toString());refreshSummary.run();}});
        }
        refreshRemote.run();
        refreshSummary.run();
        l.addView(chosen);
        l.addView(btn("🏁 COMENZAR CARRERA",v->{
            startActivity(new Intent(this,CameraActivity.class)
                .putExtra("pilotId",selectedPilot[0].id)
                .putExtra("carId",selectedCar[0].id)
                .putExtra("trackId",selectedTrack[0].id)
                .putExtra("remote",selectedRemote[0]));
            d.dismiss();
        }));
        d.setContentView(l);
        d.show();
    }
    void records(){List<Pilot>ps=db.pilots();List<Car>cs=db.cars();List<Track>ts=db.tracks();List<Session>all=db.sessions();if(all.isEmpty()){Toast.makeText(this,"No hay sesiones.",Toast.LENGTH_SHORT).show();return;}Dialog d=new Dialog(this);LinearLayout l=shell("REGISTROS");final Long[] f={null},u={null};Button bf=btn("📅 FECHA DESDE: todas",v->pickDate(x->{f[0]=x;((Button)v).setText("📅 FECHA DESDE: "+fmtDate(x));}));Button bu=btn("📅 FECHA HASTA: todas",v->pickDate(x->{u[0]=x+86400000;((Button)v).setText("📅 FECHA HASTA: "+fmtDate(x));}));l.addView(bf);l.addView(bu);Spinner spi=null,sci=null,sti=null;if(ps.size()>1)spi=filter(l,"Piloto",labelsPilots(ps));if(cs.size()>1)sci=filter(l,"Coche",labelsCars(cs));if(ts.size()>1)sti=filter(l,"Pista",labelsTracks(ts));LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);l.addView(list,new LinearLayout.LayoutParams(-1,0,1));View[] vv=new View[3];Spinner fs=spi,fc=sci,ft=sti;Button apply=btn("🔎 FILTRAR",v->{Long pi=fs==null?null:selLong(fs,ps);Long ci=fc==null?null:selLong(fc,cs);Long ti=ft==null?null:selLong(ft,ts);renderSessionRows(list,filterSessions(all,f[0],u[0],pi,ci,ti),ps,cs,ts,d);});l.addView(apply);renderSessionRows(list,all,ps,cs,ts,d);d.setContentView(l);Window w=d.getWindow();if(w!=null)w.setLayout(-1,-1);d.show();}
    Spinner filter(LinearLayout l,String label,String[] vals){l.addView(new TextView(this){{setText(label);}});Spinner s=new Spinner(this);List<String> all=new ArrayList<>();all.add("Todos");Collections.addAll(all,vals);s.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,all));l.addView(s);return s;}
    Long selLong(Spinner s,List<?> list){int p=s.getSelectedItemPosition();return p<=0?null:(list instanceof List&&list.get(p-1) instanceof Pilot?((Pilot)((List)list).get(p-1)).id:list.get(p-1) instanceof Car?((Car)((List)list).get(p-1)).id:((Track)((List)list).get(p-1)).id);}
    List<Session> filterSessions(List<Session> all,Long f,Long u,Long pi,Long ci,Long ti){List<Session> r=new ArrayList<>();for(Session s:all)if((f==null||s.started>=f)&&(u==null||s.started<u)&&(pi==null||s.pilotId==pi)&&(ci==null||s.carId==ci)&&(ti==null||s.trackId==ti))r.add(s);return r;}
    String sessionText(List<Session> ss,List<Pilot>ps,List<Car>cs,List<Track>ts){StringBuilder b=new StringBuilder();Map<Long,Pilot>p=pm(ps);Map<Long,Car>c=cm(cs);Map<Long,Track>t=tm(ts);for(Session s:ss){b.append(fmt(s.started)).append("\n").append(p.get(s.pilotId).label()).append(" · ").append(c.get(s.carId).label()).append(" · ").append(t.get(s.trackId).name).append("\n").append(s.laps).append(" vueltas · mejor ").append(String.format(Locale.getDefault(),"%.3f",s.best)).append(" s · ").append(String.format(Locale.getDefault(),"%.2f",s.distance/1000)).append(" km\nNotas: ").append(s.notes).append("\n\n");}return b.toString();}
    void renderSessionRows(LinearLayout list,List<Session> ss,List<Pilot>ps,List<Car>cs,List<Track>ts,Dialog parent){list.removeAllViews();Map<Long,Pilot>p=pm(ps);Map<Long,Car>c=cm(cs);Map<Long,Track>t=tm(ts);if(ss.isEmpty()){list.addView(new TextView(this){{setText("No hay resultados para estos filtros.");setTextSize(17);}});return;}for(Session s:ss){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(8,8,8,16);TextView tx=new TextView(this);tx.setText(fmt(s.started)+"\n"+p.get(s.pilotId).label()+" · "+c.get(s.carId).label()+" · "+t.get(s.trackId).name+"\n"+s.laps+" vueltas · mejor "+String.format(Locale.getDefault(),"%.3f s",s.best)+" · "+String.format(Locale.getDefault(),"%.2f km",s.distance/1000)+"\nNotas: "+s.notes);tx.setTextSize(16);row.addView(tx);LinearLayout actions=new LinearLayout(this);Button view=btn("VER VUELTAS",v->showSession(s));Button del=btn("BORRAR",v->confirm("¿Borrar esta sesión y todos sus tiempos?",()->{db.deleteSession(s.id);parent.dismiss();records();}));actions.addView(view,new LinearLayout.LayoutParams(0,-2,1));actions.addView(del,new LinearLayout.LayoutParams(0,-2,1));row.addView(actions);list.addView(row);}}
    void showSession(Session s){Map<Long,Pilot>p=pm(db.pilots());Map<Long,Car>c=cm(db.cars());Map<Long,Track>t=tm(db.tracks());StringBuilder b=new StringBuilder();b.append(fmt(s.started)).append("\n").append(p.get(s.pilotId).label()).append(" · ").append(c.get(s.carId).label()).append(" · ").append(t.get(s.trackId).name).append("\nMando: ").append(s.remote).append("\n\n");for(Lap x:db.laps(s.id))b.append("Vuelta ").append(x.number).append(": ").append(String.format(Locale.getDefault(),"%.3f s",x.seconds)).append("\n");b.append("\nMejor: ").append(String.format(Locale.getDefault(),"%.3f s",s.best)).append("\nMedia: ").append(String.format(Locale.getDefault(),"%.3f s",s.average)).append("\nDistancia: ").append(String.format(Locale.getDefault(),"%.2f km",s.distance/1000)).append("\nNotas: ").append(s.notes);new AlertDialog.Builder(this).setTitle("DETALLE DE CARRERA").setMessage(b.toString()).setPositiveButton("CERRAR",null).show();}
    void recordsMenu(){List<Track>ts=db.tracks();List<Pilot>ps=db.pilots();if(ts.isEmpty()){Toast.makeText(this,"Crea un circuito primero.",Toast.LENGTH_SHORT).show();return;}Dialog d=new Dialog(this);LinearLayout l=shell("RÉCORDS");Spinner t=new Spinner(this);t.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labelsTracks(ts)));l.addView(new TextView(this){{setText("Pista");}});l.addView(t);l.addView(btn("🏆 RÉCORDS ABSOLUTOS",v->showRecords(ts.get(t.getSelectedItemPosition()).id,null,ts.get(t.getSelectedItemPosition()).name)));if(ps.size()>0){Spinner p=new Spinner(this);p.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labelsPilots(ps)));l.addView(new TextView(this){{setText("Piloto");}});l.addView(p);l.addView(btn("👤 RÉCORDS POR PILOTO",v->showRecords(ts.get(t.getSelectedItemPosition()).id,ps.get(p.getSelectedItemPosition()).id,ts.get(t.getSelectedItemPosition()).name)));}d.setContentView(l);d.show();}
    void showRecords(long track,Long pilot,String name){Map<Long,Pilot>ps=pm(db.pilots());Map<Long,Car>cs=cm(db.cars());StringBuilder b=new StringBuilder();int pos=1;for(Object[] r:db.recordRows(track,pilot)){b.append(pos++).append(". ").append(ps.get((Long)r[0]).label()).append(" · ").append(cs.get((Long)r[1]).label()).append(" · ").append(String.format(Locale.getDefault(),"%.3f s",(Double)r[2])).append("\n");}AlertDialog.Builder a=new AlertDialog.Builder(this).setTitle(name).setMessage(b.length()==0?"Sin tiempos.":b.toString()).setPositiveButton("OK",null);a.show();}
    void stats(){List<Session> ss=db.sessions();if(ss.isEmpty()){Toast.makeText(this,"No hay datos todavía.",Toast.LENGTH_SHORT).show();return;}double km=0,totaltime=0;int laps=0;double best=Double.MAX_VALUE;for(Session s:ss){km+=s.distance/1000.0;laps+=s.laps;totaltime+=s.average*s.laps;if(s.best>0)best=Math.min(best,s.best);}double speed=totaltime>0?km/(totaltime/3600.0):0;AlertDialog.Builder a=new AlertDialog.Builder(this).setTitle("ESTADÍSTICAS").setMessage("Sesiones: "+ss.size()+"\nVueltas: "+laps+"\nKilómetros: "+String.format(Locale.getDefault(),"%.2f",km)+" km\nMejor vuelta: "+(best==Double.MAX_VALUE?"—":String.format(Locale.getDefault(),"%.3f s",best))+"\nVelocidad media: "+String.format(Locale.getDefault(),"%.2f km/h",speed)).setPositiveButton("CERRAR",null);a.show();}
    void calibration(){List<Track>ts=db.tracks();if(ts.isEmpty()){Toast.makeText(this,"Crea un circuito primero.",Toast.LENGTH_SHORT).show();return;}Dialog d=new Dialog(this);LinearLayout l=shell("CALIBRACIÓN");Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labelsTracks(ts)));l.addView(new TextView(this){{setText("Circuito");}});l.addView(s);l.addView(btn("🎯 ABRIR CALIBRACIÓN",v->{startActivity(new Intent(this,CameraActivity.class).putExtra("calibrationOnly",true).putExtra("trackId",ts.get(s.getSelectedItemPosition()).id));d.dismiss();}));d.setContentView(l);d.show();}
    void export(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");i.putExtra(Intent.EXTRA_TITLE,"CronoSlot.xlsx");exportLauncher.launch(i);}
    void writeExcel(Uri uri){try{WorkbookBuilder.write(this,uri,db);}catch(Exception e){Toast.makeText(this,"Error Excel: "+e.getMessage(),Toast.LENGTH_LONG).show();}}
    void confirm(String msg,Runnable yes){new AlertDialog.Builder(this).setMessage(msg).setNegativeButton("CANCELAR",null).setPositiveButton("BORRAR",(d,w)->yes.run()).show();}
    void pickDate(java.util.function.LongConsumer c){Calendar z=Calendar.getInstance();new DatePickerDialog(this,(v,y,m,day)->{Calendar x=Calendar.getInstance();x.set(y,m,day,0,0,0);x.set(Calendar.MILLISECOND,0);c.accept(x.getTimeInMillis());},z.get(Calendar.YEAR),z.get(Calendar.MONTH),z.get(Calendar.DAY_OF_MONTH)).show();}
    String fmt(long x){return new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(new Date(x));}
    String fmtDate(long x){return new SimpleDateFormat("dd/MM/yyyy",Locale.getDefault()).format(new Date(x));}
    Map<Long,Pilot> pm(List<Pilot>a){Map<Long,Pilot>m=new HashMap<>();for(Pilot x:a)m.put(x.id,x);return m;} Map<Long,Car>cm(List<Car>a){Map<Long,Car>m=new HashMap<>();for(Car x:a)m.put(x.id,x);return m;} Map<Long,Track>tm(List<Track>a){Map<Long,Track>m=new HashMap<>();for(Track x:a)m.put(x.id,x);return m;}
    String[] labelsPilots(List<Pilot> xs){String[] r=new String[xs.size()];for(int i=0;i<xs.size();i++)r[i]=xs.get(i).label();return r;}
    String[] labelsCars(List<Car> xs){String[] r=new String[xs.size()];for(int i=0;i<xs.size();i++)r[i]=xs.get(i).label();return r;}
    String[] labelsTracks(List<Track> xs){String[] r=new String[xs.size()];for(int i=0;i<xs.size();i++)r[i]=xs.get(i).name;return r;}
    Pilot findPilot(List<Pilot> xs,String q){if(q==null)return xs.isEmpty()?null:xs.get(0);for(Pilot x:xs)if(x.label().equalsIgnoreCase(q.trim()))return x;for(Pilot x:xs)if(x.label().toLowerCase(Locale.getDefault()).contains(q.trim().toLowerCase(Locale.getDefault())))return x;return xs.isEmpty()?null:xs.get(0);}
    Car findCar(List<Car> xs,String q){if(q==null)return xs.isEmpty()?null:xs.get(0);for(Car x:xs)if(x.label().equalsIgnoreCase(q.trim()))return x;for(Car x:xs)if(x.label().toLowerCase(Locale.getDefault()).contains(q.trim().toLowerCase(Locale.getDefault())))return x;return xs.isEmpty()?null:xs.get(0);}
    Track findTrack(List<Track> xs,String q){if(q==null)return xs.isEmpty()?null:xs.get(0);for(Track x:xs)if(x.name.equalsIgnoreCase(q.trim()))return x;for(Track x:xs)if(x.name.toLowerCase(Locale.getDefault()).contains(q.trim().toLowerCase(Locale.getDefault())))return x;return xs.isEmpty()?null:xs.get(0);}

}
