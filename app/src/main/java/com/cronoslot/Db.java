package com.cronoslot;

import android.content.*;
import android.database.sqlite.*;
import android.database.Cursor;
import java.util.*;

public class Db extends SQLiteOpenHelper {
 public Db(Context c){super(c,"cronoslot.db",null,1);}
 public void onCreate(SQLiteDatabase d){
  d.execSQL("CREATE TABLE pilots(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,surname TEXT,remotes TEXT,photo TEXT)");
  d.execSQL("CREATE TABLE cars(id INTEGER PRIMARY KEY AUTOINCREMENT,brand TEXT,model TEXT,chassis TEXT,frontTyre TEXT,rearTyre TEXT,braid TEXT,notes TEXT,photo TEXT)");
  d.execSQL("CREATE TABLE tracks(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,length REAL,notes TEXT,photo TEXT)");
  d.execSQL("CREATE TABLE sessions(id INTEGER PRIMARY KEY AUTOINCREMENT,pilotId INTEGER,carId INTEGER,trackId INTEGER,remote TEXT,started INTEGER,finished INTEGER,notes TEXT,laps INTEGER,best REAL,average REAL,distance REAL)");
  d.execSQL("CREATE TABLE laps(id INTEGER PRIMARY KEY AUTOINCREMENT,sessionId INTEGER,number INTEGER,seconds REAL)");
 }
 public void onUpgrade(SQLiteDatabase d,int o,int n){}
 private List<String> splitRemotes(String s){List<String> r=new ArrayList<>();if(s==null)return r;for(String x:s.split("[,\\n]"))if(!x.trim().isEmpty())r.add(x.trim());return r;}
 public List<Pilot> pilots(){List<Pilot> r=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,name,surname,remotes,photo FROM pilots ORDER BY surname,name",null);while(c.moveToNext())r.add(new Pilot(c.getLong(0),c.getString(1),c.getString(2),splitRemotes(c.getString(3)),c.getString(4)));c.close();return r;}
 public List<Car> cars(){List<Car> r=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,brand,model,chassis,frontTyre,rearTyre,braid,notes,photo FROM cars ORDER BY brand,model",null);while(c.moveToNext())r.add(new Car(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6),c.getString(7),c.getString(8)));c.close();return r;}
 public List<Track> tracks(){List<Track> r=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,name,length,notes,photo FROM tracks ORDER BY name",null);while(c.moveToNext())r.add(new Track(c.getLong(0),c.getString(1),c.getDouble(2),c.getString(3),c.getString(4)));c.close();return r;}
 public List<Session> sessions(){List<Session> r=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,pilotId,carId,trackId,remote,started,finished,notes,laps,best,average,distance FROM sessions ORDER BY started DESC",null);while(c.moveToNext())r.add(new Session(c.getLong(0),c.getLong(1),c.getLong(2),c.getLong(3),c.getString(4),c.getLong(5),c.getLong(6),c.getString(7),c.getInt(8),c.getDouble(9),c.getDouble(10),c.getDouble(11)));c.close();return r;}
 public List<Lap> laps(long sid){List<Lap> r=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,sessionId,number,seconds FROM laps WHERE sessionId=? ORDER BY number",new String[]{""+sid});while(c.moveToNext())r.add(new Lap(c.getLong(0),c.getLong(1),c.getInt(2),c.getDouble(3)));c.close();return r;}
 private long insert(String table,ContentValues v){return getWritableDatabase().insert(table,null,v);}
 public long addPilot(Pilot p){ContentValues v=new ContentValues();v.put("name",p.name);v.put("surname",p.surname);v.put("remotes",String.join("\n",p.remotes));v.put("photo",p.photo);return insert("pilots",v);}
 public void updatePilot(Pilot p){ContentValues v=new ContentValues();v.put("name",p.name);v.put("surname",p.surname);v.put("remotes",String.join("\n",p.remotes));v.put("photo",p.photo);getWritableDatabase().update("pilots",v,"id=?",new String[]{""+p.id});}
 public void deletePilot(long id){getWritableDatabase().delete("pilots","id=?",new String[]{""+id});}
 public long addCar(Car p){ContentValues v=new ContentValues();v.put("brand",p.brand);v.put("model",p.model);v.put("chassis",p.chassis);v.put("frontTyre",p.frontTyre);v.put("rearTyre",p.rearTyre);v.put("braid",p.braid);v.put("notes",p.notes);v.put("photo",p.photo);return insert("cars",v);}
 public void updateCar(Car p){ContentValues v=new ContentValues();v.put("brand",p.brand);v.put("model",p.model);v.put("chassis",p.chassis);v.put("frontTyre",p.frontTyre);v.put("rearTyre",p.rearTyre);v.put("braid",p.braid);v.put("notes",p.notes);v.put("photo",p.photo);getWritableDatabase().update("cars",v,"id=?",new String[]{""+p.id});}
 public void deleteCar(long id){getWritableDatabase().delete("cars","id=?",new String[]{""+id});}
 public long addTrack(Track p){ContentValues v=new ContentValues();v.put("name",p.name);v.put("length",p.length);v.put("notes",p.notes);v.put("photo",p.photo);return insert("tracks",v);}
 public void updateTrack(Track p){ContentValues v=new ContentValues();v.put("name",p.name);v.put("length",p.length);v.put("notes",p.notes);v.put("photo",p.photo);getWritableDatabase().update("tracks",v,"id=?",new String[]{""+p.id});}
 public void deleteTrack(long id){getWritableDatabase().delete("tracks","id=?",new String[]{""+id});}
 public long addSession(long pilot,long car,long track,String remote,long started,String notes,List<Double> laps){
  Track t=null; for(Track x:tracks())if(x.id==track)t=x; double best=Double.MAX_VALUE,sum=0; for(double v:laps){best=Math.min(best,v);sum+=v;} double avg=laps.isEmpty()?0:sum/laps.size();
  ContentValues v=new ContentValues();v.put("pilotId",pilot);v.put("carId",car);v.put("trackId",track);v.put("remote",remote);v.put("started",started);v.put("finished",System.currentTimeMillis());v.put("notes",notes);v.put("laps",laps.size());v.put("best",laps.isEmpty()?0:best);v.put("average",avg);v.put("distance",(t==null?0:t.length)*laps.size());
  long sid=insert("sessions",v);for(int i=0;i<laps.size();i++){ContentValues lv=new ContentValues();lv.put("sessionId",sid);lv.put("number",i+1);lv.put("seconds",laps.get(i));insert("laps",lv);}return sid;
 }
 public void deleteSession(long id){getWritableDatabase().delete("laps","sessionId=?",new String[]{""+id});getWritableDatabase().delete("sessions","id=?",new String[]{""+id});}
 public Double bestTrack(long track){Cursor c=getReadableDatabase().rawQuery("SELECT MIN(l.seconds) FROM laps l JOIN sessions s ON s.id=l.sessionId WHERE s.trackId=?",new String[]{""+track});Double x=null;if(c.moveToFirst()&&!c.isNull(0))x=c.getDouble(0);c.close();return x;}
 public Double bestCombo(long track,long pilot,long car){Cursor c=getReadableDatabase().rawQuery("SELECT MIN(l.seconds) FROM laps l JOIN sessions s ON s.id=l.sessionId WHERE s.trackId=? AND s.pilotId=? AND s.carId=?",new String[]{""+track,""+pilot,""+car});Double x=null;if(c.moveToFirst()&&!c.isNull(0))x=c.getDouble(0);c.close();return x;}
 public List<Object[]> recordRows(long track,Long pilot){String sql=pilot==null?"SELECT s.pilotId,s.carId,MIN(l.seconds) FROM sessions s JOIN laps l ON l.sessionId=s.id WHERE s.trackId=? GROUP BY s.pilotId,s.carId ORDER BY MIN(l.seconds)":"SELECT s.pilotId,s.carId,MIN(l.seconds) FROM sessions s JOIN laps l ON l.sessionId=s.id WHERE s.trackId=? AND s.pilotId=? GROUP BY s.pilotId,s.carId ORDER BY MIN(l.seconds)";String[] args=pilot==null?new String[]{""+track}:new String[]{""+track,""+pilot};Cursor c=getReadableDatabase().rawQuery(sql,args);List<Object[]> r=new ArrayList<>();while(c.moveToNext())r.add(new Object[]{c.getLong(0),c.getLong(1),c.getDouble(2)});c.close();return r;}
}
