package com.cronoslot;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.*;

public class Db extends SQLiteOpenHelper {
    public Db(Context c){super(c,"cronoslot.db",null,2);}
    @Override public void onCreate(SQLiteDatabase d){
        d.execSQL("CREATE TABLE pilots(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,surname TEXT,remotes TEXT,photo TEXT)");
        d.execSQL("CREATE TABLE cars(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,brand TEXT,model TEXT,chassis TEXT,frontTyre TEXT,rearTyre TEXT,braid TEXT,notes TEXT,photo TEXT)");
        d.execSQL("CREATE TABLE tracks(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,length REAL,minLap REAL DEFAULT 0,notes TEXT,photo TEXT)");
        d.execSQL("CREATE TABLE sessions(id INTEGER PRIMARY KEY AUTOINCREMENT,pilotId INTEGER,carId INTEGER,trackId INTEGER,remote TEXT,started INTEGER,finished INTEGER,notes TEXT,laps INTEGER,best REAL,average REAL,distance REAL)");
        d.execSQL("CREATE TABLE laps(id INTEGER PRIMARY KEY AUTOINCREMENT,sessionId INTEGER,number INTEGER,seconds REAL)");
    }
    @Override public void onUpgrade(SQLiteDatabase d,int o,int n){ if(o<2){ try{ d.execSQL("ALTER TABLE tracks ADD COLUMN minLap REAL DEFAULT 0"); }catch(Exception ignored){} } }
    private String s(Cursor c,int i){return c.isNull(i)?"":c.getString(i);}
    public List<Pilot> pilots(){List<Pilot> r=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,name,surname,remotes,photo FROM pilots ORDER BY surname,name",null);while(c.moveToNext())r.add(new Pilot(c.getLong(0),s(c,1),s(c,2),s(c,3),s(c,4)));c.close();return r;}
    public List<Car> cars(){List<Car> r=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,name,brand,model,chassis,frontTyre,rearTyre,braid,notes,photo FROM cars ORDER BY name",null);while(c.moveToNext())r.add(new Car(c.getLong(0),s(c,1),s(c,2),s(c,3),s(c,4),s(c,5),s(c,6),s(c,7),s(c,8),s(c,9)));c.close();return r;}
    public List<Track> tracks(){List<Track> r=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,name,length,minLap,notes,photo FROM tracks ORDER BY name",null);while(c.moveToNext())r.add(new Track(c.getLong(0),s(c,1),c.getDouble(2),c.getDouble(3),s(c,4),s(c,5)));c.close();return r;}
    public void addPilot(String n,String su,String r){ContentValues v=new ContentValues();v.put("name",n);v.put("surname",su);v.put("remotes",r);getWritableDatabase().insert("pilots",null,v);}
    public void updatePilot(long id,String n,String su,String r){ContentValues v=new ContentValues();v.put("name",n);v.put("surname",su);v.put("remotes",r);getWritableDatabase().update("pilots",v,"id=?",new String[]{""+id});}
    public String getPilotPhoto(long id){Cursor c=getReadableDatabase().rawQuery("SELECT photo FROM pilots WHERE id=?",new String[]{String.valueOf(id)});String x=null;if(c.moveToFirst()&&!c.isNull(0))x=c.getString(0);c.close();return x;}
    public String getCarPhoto(long id){Cursor c=getReadableDatabase().rawQuery("SELECT photo FROM cars WHERE id=?",new String[]{String.valueOf(id)});String x=null;if(c.moveToFirst()&&!c.isNull(0))x=c.getString(0);c.close();return x;}
    public String getTrackPhoto(long id){Cursor c=getReadableDatabase().rawQuery("SELECT photo FROM tracks WHERE id=?",new String[]{String.valueOf(id)});String x=null;if(c.moveToFirst()&&!c.isNull(0))x=c.getString(0);c.close();return x;}

    public void updatePilotPhoto(long id,String photo){ContentValues v=new ContentValues();v.put("photo",photo);getWritableDatabase().update("pilots",v,"id=?",new String[]{String.valueOf(id)});}
    public void updateCarPhoto(long id,String photo){ContentValues v=new ContentValues();v.put("photo",photo);getWritableDatabase().update("cars",v,"id=?",new String[]{String.valueOf(id)});}
    public void updateTrackPhoto(long id,String photo){ContentValues v=new ContentValues();v.put("photo",photo);getWritableDatabase().update("tracks",v,"id=?",new String[]{String.valueOf(id)});}

    public void deletePilot(long id){getWritableDatabase().delete("pilots","id=?",new String[]{""+id});}
    public void addCar(String n,String b,String m,String ch,String f,String rr,String br,String no){ContentValues v=new ContentValues();v.put("name",n);v.put("brand",b);v.put("model",m);v.put("chassis",ch);v.put("frontTyre",f);v.put("rearTyre",rr);v.put("braid",br);v.put("notes",no);getWritableDatabase().insert("cars",null,v);}
    public void updateCar(long id,String n,String b,String m,String ch,String f,String rr,String br,String no){ContentValues v=new ContentValues();v.put("name",n);v.put("brand",b);v.put("model",m);v.put("chassis",ch);v.put("frontTyre",f);v.put("rearTyre",rr);v.put("braid",br);v.put("notes",no);getWritableDatabase().update("cars",v,"id=?",new String[]{""+id});}
    public void deleteCar(long id){getWritableDatabase().delete("cars","id=?",new String[]{""+id});}
    public void addTrack(String n,double l,double min,String no){ContentValues v=new ContentValues();v.put("name",n);v.put("length",l);v.put("minLap",min);v.put("notes",no);getWritableDatabase().insert("tracks",null,v);}
    public void updateTrack(long id,String n,double l,double min,String no){ContentValues v=new ContentValues();v.put("name",n);v.put("length",l);v.put("minLap",min);v.put("notes",no);getWritableDatabase().update("tracks",v,"id=?",new String[]{""+id});}
    public void deleteTrack(long id){getWritableDatabase().delete("tracks","id=?",new String[]{""+id});}
    public void addSession(long pilot,long car,long track,String remote,long started,String notes,List<Double> laps){if(laps==null||laps.isEmpty())return;double best=Collections.min(laps),sum=0;for(double x:laps)sum+=x;double avg=sum/laps.size();Track t=null;for(Track x:tracks())if(x.id==track)t=x;double dist=(t==null?0:t.length)*laps.size();ContentValues v=new ContentValues();v.put("pilotId",pilot);v.put("carId",car);v.put("trackId",track);v.put("remote",remote);v.put("started",started);v.put("finished",System.currentTimeMillis());v.put("notes",notes);v.put("laps",laps.size());v.put("best",best);v.put("average",avg);v.put("distance",dist);long sid=getWritableDatabase().insert("sessions",null,v);int i=1;for(double x:laps){ContentValues lv=new ContentValues();lv.put("sessionId",sid);lv.put("number",i++);lv.put("seconds",x);getWritableDatabase().insert("laps",null,lv);}}
    public List<Session> sessions(){List<Session> r=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,pilotId,carId,trackId,remote,started,finished,notes,laps,best,average,distance FROM sessions ORDER BY started DESC",null);while(c.moveToNext())r.add(new Session(c.getLong(0),c.getLong(1),c.getLong(2),c.getLong(3),s(c,4),c.getLong(5),c.isNull(6)?null:c.getLong(6),s(c,7),c.getInt(8),c.getDouble(9),c.getDouble(10),c.getDouble(11)));c.close();return r;}
    public List<Lap> laps(long sid){List<Lap> r=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,sessionId,number,seconds FROM laps WHERE sessionId=? ORDER BY number",new String[]{""+sid});while(c.moveToNext())r.add(new Lap(c.getLong(0),c.getLong(1),c.getInt(2),c.getDouble(3)));c.close();return r;}
    public List<Object[]> recordRows(long track,Long pilot){String sql=pilot==null?"SELECT s.pilotId,s.carId,MIN(l.seconds) FROM sessions s JOIN laps l ON l.sessionId=s.id WHERE s.trackId=? GROUP BY s.pilotId,s.carId ORDER BY MIN(l.seconds)":"SELECT s.pilotId,s.carId,MIN(l.seconds) FROM sessions s JOIN laps l ON l.sessionId=s.id WHERE s.trackId=? AND s.pilotId=? GROUP BY s.pilotId,s.carId ORDER BY MIN(l.seconds)";String[] a=pilot==null?new String[]{""+track}:new String[]{""+track,""+pilot};Cursor c=getReadableDatabase().rawQuery(sql,a);List<Object[]>r=new ArrayList<>();while(c.moveToNext())r.add(new Object[]{c.getLong(0),c.getLong(1),c.getDouble(2)});c.close();return r;}
    public Double bestTrack(long track){
        Cursor c=getReadableDatabase().rawQuery(
            "SELECT MIN(l.seconds) FROM laps l JOIN sessions s ON s.id=l.sessionId WHERE s.trackId=?",
            new String[]{String.valueOf(track)}
        );
        Double x=null;
        if(c.moveToFirst() && !c.isNull(0)) x=c.getDouble(0);
        c.close();
        return x;
    }


    public Double bestCarTrack(long track,long car){
        Cursor c=getReadableDatabase().rawQuery(
            "SELECT MIN(l.seconds) FROM laps l JOIN sessions s ON s.id=l.sessionId WHERE s.trackId=? AND s.carId=?",
            new String[]{String.valueOf(track),String.valueOf(car)}
        );
        Double x=null;
        if(c.moveToFirst() && !c.isNull(0)) x=c.getDouble(0);
        c.close();
        return x;
    }


    public List<Object[]> top20Absolute(long trackId) {
        List<Object[]> r = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT l.seconds,s.pilotId,s.carId,s.started " +
            "FROM laps l JOIN sessions s ON s.id=l.sessionId " +
            "WHERE s.trackId=? ORDER BY l.seconds ASC LIMIT 13",
            new String[]{String.valueOf(trackId)}
        );
        while (c.moveToNext()) {
            r.add(new Object[]{c.getDouble(0), c.getLong(1), c.getLong(2), c.getLong(3)});
        }
        c.close();
        return r;
    }

    public List<Object[]> top20PilotTrack(long trackId, long pilotId) {
        List<Object[]> r = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT l.seconds,s.carId,s.started " +
            "FROM laps l JOIN sessions s ON s.id=l.sessionId " +
            "WHERE s.trackId=? AND s.pilotId=? ORDER BY l.seconds ASC LIMIT 13",
            new String[]{String.valueOf(trackId), String.valueOf(pilotId)}
        );
        while (c.moveToNext()) {
            r.add(new Object[]{c.getDouble(0), c.getLong(1), c.getLong(2)});
        }
        c.close();
        return r;
    }

    public List<Object[]> top20Exact(long trackId, long pilotId, long carId) {
        List<Object[]> r = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT l.seconds,s.started " +
            "FROM laps l JOIN sessions s ON s.id=l.sessionId " +
            "WHERE s.trackId=? AND s.pilotId=? AND s.carId=? ORDER BY l.seconds ASC LIMIT 13",
            new String[]{String.valueOf(trackId), String.valueOf(pilotId), String.valueOf(carId)}
        );
        while (c.moveToNext()) {
            r.add(new Object[]{c.getDouble(0), c.getLong(1)});
        }
        c.close();
        return r;
    }

    public Double bestCombo(long track,long pilot,long car){Cursor c=getReadableDatabase().rawQuery("SELECT MIN(l.seconds) FROM laps l JOIN sessions s ON s.id=l.sessionId WHERE s.trackId=? AND s.pilotId=? AND s.carId=?",new String[]{""+track,""+pilot,""+car});Double x=null;if(c.moveToFirst()&&!c.isNull(0))x=c.getDouble(0);c.close();return x;}
}
