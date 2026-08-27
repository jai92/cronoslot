package com.cronoslot

import android.content.*
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class Db(ctx:Context):SQLiteOpenHelper(ctx,"cronoslot.db",null,1){
    override fun onCreate(d:SQLiteDatabase){
        d.execSQL("CREATE TABLE pilots(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,surname TEXT NOT NULL,remotes TEXT NOT NULL,photo TEXT)")
        d.execSQL("CREATE TABLE cars(id INTEGER PRIMARY KEY AUTOINCREMENT,brand TEXT NOT NULL,model TEXT NOT NULL,chassis TEXT,frontTyre TEXT,rearTyre TEXT,braid TEXT,notes TEXT,photo TEXT)")
        d.execSQL("CREATE TABLE tracks(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,length REAL NOT NULL,notes TEXT,photo TEXT)")
        d.execSQL("CREATE TABLE sessions(id INTEGER PRIMARY KEY AUTOINCREMENT,pilotId INTEGER,carId INTEGER,trackId INTEGER,remote TEXT,started INTEGER,finished INTEGER,notes TEXT,laps INTEGER,best REAL,average REAL,distance REAL)")
        d.execSQL("CREATE TABLE laps(id INTEGER PRIMARY KEY AUTOINCREMENT,sessionId INTEGER,number INTEGER,seconds REAL)")
    }
    override fun onUpgrade(d:SQLiteDatabase,o:Int,n:Int){}
    fun pilots():List<Pilot>{
        val c=readableDatabase.rawQuery("SELECT id,name,surname,remotes,photo FROM pilots ORDER BY surname,name",null)
        val r=mutableListOf<Pilot>(); while(c.moveToNext()) r.add(Pilot(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4))); c.close(); return r
    }
    fun cars():List<Car>{
        val c=readableDatabase.rawQuery("SELECT id,brand,model,chassis,frontTyre,rearTyre,braid,notes,photo FROM cars ORDER BY brand,model",null)
        val r=mutableListOf<Car>(); while(c.moveToNext()) r.add(Car(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6),c.getString(7),c.getString(8))); c.close(); return r
    }
    fun tracks():List<Track>{
        val c=readableDatabase.rawQuery("SELECT id,name,length,notes,photo FROM tracks ORDER BY name",null)
        val r=mutableListOf<Track>(); while(c.moveToNext()) r.add(Track(c.getLong(0),c.getString(1),c.getDouble(2),c.getString(3),c.getString(4))); c.close(); return r
    }
    fun addPilot(p:Pilot)=writableDatabase.execSQL("INSERT INTO pilots(name,surname,remotes,photo) VALUES(?,?,?,?)",arrayOf(p.name,p.surname,p.remotes,p.photo))
    fun addCar(c:Car)=writableDatabase.execSQL("INSERT INTO cars(brand,model,chassis,frontTyre,rearTyre,braid,notes,photo) VALUES(?,?,?,?,?,?,?,?)",arrayOf(c.brand,c.model,c.chassis,c.frontTyre,c.rearTyre,c.braid,c.notes,c.photo))
    fun addTrack(t:Track)=writableDatabase.execSQL("INSERT INTO tracks(name,length,notes,photo) VALUES(?,?,?,?)",arrayOf(t.name,t.length,t.notes,t.photo))
    fun deletePilot(id:Long)=writableDatabase.delete("pilots","id=?",arrayOf(id.toString()))
    fun deleteCar(id:Long)=writableDatabase.delete("cars","id=?",arrayOf(id.toString()))
    fun deleteTrack(id:Long)=writableDatabase.delete("tracks","id=?",arrayOf(id.toString()))
    fun insertSession(p:Long,c:Long,t:Long,r:String,start:Long,notes:String,laps:Int,best:Double,avg:Double,distance:Double):Long{
        val v=ContentValues(); v.put("pilotId",p);v.put("carId",c);v.put("trackId",t);v.put("remote",r);v.put("started",start);v.put("finished",System.currentTimeMillis());v.put("notes",notes);v.put("laps",laps);v.put("best",best);v.put("average",avg);v.put("distance",distance)
        return writableDatabase.insert("sessions",null,v)
    }
    fun insertLap(s:Long,n:Int,sec:Double){val v=ContentValues();v.put("sessionId",s);v.put("number",n);v.put("seconds",sec);writableDatabase.insert("laps",null,v)}
    fun sessions():List<Session>{
        val c=readableDatabase.rawQuery("SELECT id,pilotId,carId,trackId,remote,started,finished,notes,laps,best,average,distance FROM sessions ORDER BY started DESC",null)
        val r=mutableListOf<Session>();while(c.moveToNext())r.add(Session(c.getLong(0),c.getLong(1),c.getLong(2),c.getLong(3),c.getString(4),c.getLong(5),if(c.isNull(6))null else c.getLong(6),c.getString(7),c.getInt(8),c.getDouble(9),c.getDouble(10),c.getDouble(11)));c.close();return r
    }
    fun laps(session:Long):List<Lap>{val c=readableDatabase.rawQuery("SELECT id,sessionId,number,seconds FROM laps WHERE sessionId=? ORDER BY number",arrayOf(session.toString()));val r=mutableListOf<Lap>();while(c.moveToNext())r.add(Lap(c.getLong(0),c.getLong(1),c.getInt(2),c.getDouble(3)));c.close();return r}
    fun recordRows(trackId:Long,pilotId:Long?=null):List<Array<String>>{
        val sql=if(pilotId==null) "SELECT s.pilotId,s.carId,MIN(l.seconds) FROM sessions s JOIN laps l ON l.sessionId=s.id WHERE s.trackId=? GROUP BY s.pilotId,s.carId ORDER BY MIN(l.seconds)" else "SELECT s.pilotId,s.carId,MIN(l.seconds) FROM sessions s JOIN laps l ON l.sessionId=s.id WHERE s.trackId=? AND s.pilotId=? GROUP BY s.pilotId,s.carId ORDER BY MIN(l.seconds)"
        val args=if(pilotId==null) arrayOf(trackId.toString()) else arrayOf(trackId.toString(),pilotId.toString())
        val c=readableDatabase.rawQuery(sql,args); val r=mutableListOf<Array<String>>();while(c.moveToNext())r.add(arrayOf(c.getString(0),c.getString(1),c.getString(2)));c.close();return r
    }
}
