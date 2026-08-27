package com.cronoslot;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.*;

public class Db extends SQLiteOpenHelper {
    public Db(Context c){super(c,"cronoslot.db",null,1);}
    @Override public void onCreate(SQLiteDatabase d){
        d.execSQL("CREATE TABLE pilots(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,surname TEXT,remotes TEXT,photo TEXT)");
        d.execSQL("CREATE TABLE cars(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,brand TEXT,model TEXT,chassis TEXT,frontTyre TEXT,rearTyre TEXT,braid TEXT,notes TEXT,photo TEXT)");
        d.execSQL("CREATE TABLE tracks(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,length REAL,notes TEXT,photo TEXT)");
        d.execSQL("CREATE TABLE sessions(id INTEGER PRIMARY KEY AUTOINCREMENT,pilotId INTEGER,carId INTEGER,trackId INTEGER,remote TEXT,started INTEGER,finished INTEGER,notes TEXT,laps INTEGER,best REAL,average REAL,distance REAL)");
        d.execSQL("CREATE TABLE laps(id INTEGER PRIMARY KEY AUTOINCREMENT,sessionId INTEGER,number INTEGER,seconds REAL)");
    }
    @Override public void onUpgrade(SQLiteDatabase d,int o,int n){}
    private String n(Cursor c,int i){return c.isNull(i)?"":c.getString(i);}
    public List<Pilot> pilots(){List<Pilot> r=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,name,surname,remotes,photo FROM pilots ORDER BY surname,name",null);while(c.moveToNext())r.add(new Pilot(c.getLong(0),n(c,1),n(c,2),n(c,3),n(c,4)));c.close();return r;}
    public List<Car> cars(){List<Car> r=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,name,brand,model,chassis,frontTyre,rearTyre,braid,notes,photo FROM cars ORDER BY name",null);while(c.moveToNext())r.add(new Car(c.getLong(0),n(c,1),n(c,2),n(c,3),n(c,4),n(c,5),n(c,6),n(c,7),n(c,8),n(c,9)));c.close();return r;}
    public List<Track> tracks(){List<Track> r=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,name,length,notes,photo FROM tracks ORDER BY name",null);while(c.moveToNext())r.add(new Track(c.getLong(0),n(c,1),c.getDouble(2),n(c,3),n(c,4)));c.close();return r;}
    public void addPilot(String a,String b,String c){ContentValues v=new ContentValues();v.put("name",a);v.put("surname",b);v.put("remotes",c);getWritableDatabase().insert("pilots",null,v);}
    public void addCar(String name,String brand,String model,String chassis,String front,String rear,String braid,String notes){ContentValues v=new ContentValues();v.put("name",name);v.put("brand",brand);v.put("model",model);v.put("chassis",chassis);v.put("frontTyre",front);v.put("rearTyre",rear);v.put("braid",braid);v.put("notes",notes);getWritableDatabase().insert("cars",null,v);}
    public void addTrack(String name,double len,String notes){ContentValues v=new ContentValues();v.put("name",name);v.put("length",len);v.put("notes",notes);getWritableDatabase().insert("tracks",null,v);}
    public void addSession(long pilot,long car,long track,String remote,long started,String notes,List<Double> laps){
        double best=laps.stream().mapToDouble(x->x).min().orElse(0),avg=laps.stream().mapToDouble(x->x).average().orElse(0);
        Track t=null;for(Track x:tracks())if(x.id==track){t=x;break;}
        double distance=(t==null?0:t.length)*laps.size();
        ContentValues v=new ContentValues();v.put("pilotId",pilot);v.put("carId",car);v.put("trackId",track);v.put("remote",remote);v.put("started",started);v.put("finished",System.currentTimeMillis());v.put("notes",notes);v.put("laps",laps.size());v.put("best",best);v.put("average",avg);v.put("distance",distance);
        long sid=getWritableDatabase().insert("sessions",null,v);
        int i=1;for(double sec:laps){ContentValues lv=new ContentValues();lv.put("sessionId",sid);lv.put("number",i++);lv.put("seconds",sec);getWritableDatabase().insert("laps",null,lv);}
    }
    public List<Session> sessions(){List<Session> r=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,pilotId,carId,trackId,remote,started,finished,notes,laps,best,average,distance FROM sessions ORDER BY started DESC",null);while(c.moveToNext())r.add(new Session(c.getLong(0),c.getLong(1),c.getLong(2),c.getLong(3),n(c,4),c.getLong(5),c.isNull(6)?null:c.getLong(6),n(c,7),c.getInt(8),c.getDouble(9),c.getDouble(10),c.getDouble(11)));c.close();return r;}
    public List<Object[]> recordRows(long track,Long pilot){String sql=pilot==null?"SELECT s.pilotId,s.carId,MIN(l.seconds) FROM sessions s JOIN laps l ON l.sessionId=s.id WHERE s.trackId=? GROUP BY s.pilotId,s.carId ORDER BY MIN(l.seconds)":"SELECT s.pilotId,s.carId,MIN(l.seconds) FROM sessions s JOIN laps l ON l.sessionId=s.id WHERE s.trackId=? AND s.pilotId=? GROUP BY s.pilotId,s.carId ORDER BY MIN(l.seconds)";String[] a=pilot==null?new String[]{""+track}:new String[]{""+track,""+pilot};Cursor c=getReadableDatabase().rawQuery(sql,a);List<Object[]>r=new ArrayList<>();while(c.moveToNext())r.add(new Object[]{c.getLong(0),c.getLong(1),c.getDouble(2)});c.close();return r;}
    public Double bestTrack(long track){Cursor c=getReadableDatabase().rawQuery("SELECT MIN(l.seconds) FROM laps l JOIN sessions s ON s.id=l.sessionId WHERE s.trackId=?",new String[]{""+track});Double x=null;if(c.moveToFirst()&&!c.isNull(0))x=c.getDouble(0);c.close();return x;}
    public Double bestCombo(long track,long pilot,long car){Cursor c=getReadableDatabase().rawQuery("SELECT MIN(l.seconds) FROM laps l JOIN sessions s ON s.id=l.sessionId WHERE s.trackId=? AND s.pilotId=? AND s.carId=?",new String[]{""+track,""+pilot,""+car});Double x=null;if(c.moveToFirst()&&!c.isNull(0))x=c.getDouble(0);c.close();return x;}
}
