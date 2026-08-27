package com.cronoslot;
public class Session {
 public long id,pilotId,carId,trackId,started,finished; public String remote,notes; public int laps; public double best,average,distance;
 public Session(long id,long pilotId,long carId,long trackId,String remote,long started,long finished,String notes,int laps,double best,double average,double distance){
  this.id=id;this.pilotId=pilotId;this.carId=carId;this.trackId=trackId;this.remote=remote;this.started=started;this.finished=finished;this.notes=notes;this.laps=laps;this.best=best;this.average=average;this.distance=distance;
 }
}
