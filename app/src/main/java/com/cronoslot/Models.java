package com.cronoslot;
class Pilot { long id; String name,surname,remotes,photo; Pilot(long i,String n,String s,String r,String p){id=i;name=n;surname=s;remotes=r;photo=p;} }
class Car { long id; String name,brand,model,chassis,frontTyre,rearTyre,braid,notes,photo; Car(long i,String n,String b,String m,String ch,String f,String rr,String br,String no,String p){id=i;name=n;brand=b;model=m;chassis=ch;frontTyre=f;rearTyre=rr;braid=br;notes=no;photo=p;} }
class Track { long id; String name; double length; String notes,photo; Track(long i,String n,double l,String no,String p){id=i;name=n;length=l;notes=no;photo=p;} }
class Session { long id,pilotId,carId,trackId,started; Long finished; String remote,notes; int laps; double best,average,distance; Session(long i,long p,long c,long t,String r,long st,Long fi,String no,int la,double be,double av,double di){id=i;pilotId=p;carId=c;trackId=t;remote=r;started=st;finished=fi;notes=no;laps=la;best=be;average=av;distance=di;} }
