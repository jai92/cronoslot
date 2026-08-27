package com.cronoslot;

import android.content.*;
import android.net.Uri;
import java.io.*;
import java.text.*;
import java.util.*;
import org.apache.poi.xssf.usermodel.*;

public class WorkbookBuilder {
 public static void write(Context ctx,Uri uri,Db db)throws Exception{
  List<Pilot> ps=db.pilots(); List<Car> cs=db.cars(); List<Track> ts=db.tracks(); List<Session> sessions=db.sessions();
  Map<Long,Pilot> pm=new HashMap<>(); Map<Long,Car> cm=new HashMap<>(); Map<Long,Track> tm=new HashMap<>();
  for(Pilot x:ps)pm.put(x.id,x); for(Car x:cs)cm.put(x.id,x); for(Track x:ts)tm.put(x.id,x);
  XSSFWorkbook wb=new XSSFWorkbook(); SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault());

  XSSFSheet resumen=wb.createSheet("Resumen");
  String[] rh={"Circuito","Fecha","Piloto","Mando","Coche","Vueltas","Mejor vuelta (s)","Media (s)","Velocidad media (km/h)","Distancia (m)","Notas"};
  XSSFRow hr=resumen.createRow(0); for(int i=0;i<rh.length;i++)hr.createCell(i).setCellValue(rh[i]);
  Collections.sort(sessions,Comparator.comparing((Session x)->tm.containsKey(x.trackId)?tm.get(x.trackId).name:""));
  int rr=1;
  for(Session x:sessions){Track t=tm.get(x.trackId);Pilot p=pm.get(x.pilotId);Car c=cm.get(x.carId);double speed=(x.average>0&&t!=null)?(t.length/x.average*3.6):0;
   String[] v={t==null?"":t.name,sdf.format(new Date(x.started)),p==null?"":p.label(),x.remote==null?"":x.remote,c==null?"":c.label(),String.valueOf(x.laps),String.format(Locale.getDefault(),"%.3f",x.best),String.format(Locale.getDefault(),"%.3f",x.average),String.format(Locale.getDefault(),"%.2f",speed),String.format(Locale.getDefault(),"%.2f",x.distance),x.notes==null?"":x.notes};
   XSSFRow row=resumen.createRow(rr++);for(int i=0;i<v.length;i++)row.createCell(i).setCellValue(v[i]);
  }

  XSSFSheet sp=wb.createSheet("Pilotos"); String[] ph={"ID","Nombre","Apellidos","Mandos","Foto"}; for(int i=0;i<ph.length;i++)sp.createRow(0).createCell(i).setCellValue(ph[i]);
  int r=1; for(Pilot x:ps){XSSFRow row=sp.createRow(r++);row.createCell(0).setCellValue(x.id);row.createCell(1).setCellValue(x.name);row.createCell(2).setCellValue(x.surname);row.createCell(3).setCellValue(String.join(", ",x.remotes));row.createCell(4).setCellValue(x.photo==null?"":x.photo);}
  XSSFSheet sc=wb.createSheet("Coches"); String[] ch={"ID","Marca","Modelo","Chasis","Neumáticos delanteros","Neumáticos traseros","Trencilla","Notas","Foto"}; for(int i=0;i<ch.length;i++)sc.createRow(0).createCell(i).setCellValue(ch[i]);
  r=1;for(Car x:cs){XSSFRow row=sc.createRow(r++);String[] v={""+x.id,x.brand,x.model,x.chassis,x.frontTyre,x.rearTyre,x.braid,x.notes,x.photo==null?"":x.photo};for(int i=0;i<v.length;i++)row.createCell(i).setCellValue(v[i]);}
  XSSFSheet st=wb.createSheet("Circuitos"); String[] th={"ID","Nombre","Longitud (m)","Notas","Foto"}; for(int i=0;i<th.length;i++)st.createRow(0).createCell(i).setCellValue(th[i]);
  r=1;for(Track x:ts){XSSFRow row=st.createRow(r++);row.createCell(0).setCellValue(x.id);row.createCell(1).setCellValue(x.name);row.createCell(2).setCellValue(x.length);row.createCell(3).setCellValue(x.notes);row.createCell(4).setCellValue(x.photo==null?"":x.photo);}
  XSSFSheet se=wb.createSheet("Sesiones"); String[] seh={"ID","Fecha","Piloto","Mando","Coche","Circuito","Vueltas","Mejor (s)","Media (s)","Distancia (m)","Notas"}; for(int i=0;i<seh.length;i++)se.createRow(0).createCell(i).setCellValue(seh[i]);
  r=1;for(Session x:sessions){XSSFRow row=se.createRow(r++);Pilot p=pm.get(x.pilotId);Car c=cm.get(x.carId);Track t=tm.get(x.trackId);String[] v={""+x.id,sdf.format(new Date(x.started)),p==null?"":p.label(),x.remote==null?"":x.remote,c==null?"":c.label(),t==null?"":t.name,""+x.laps,String.format(Locale.getDefault(),"%.3f",x.best),String.format(Locale.getDefault(),"%.3f",x.average),String.format(Locale.getDefault(),"%.2f",x.distance),x.notes==null?"":x.notes};for(int i=0;i<v.length;i++)row.createCell(i).setCellValue(v[i]);}
  XSSFSheet la=wb.createSheet("Vueltas"); String[] lah={"Sesión ID","Vuelta","Tiempo (s)"}; for(int i=0;i<lah.length;i++)la.createRow(0).createCell(i).setCellValue(lah[i]);
  r=1;for(Session x:sessions)for(Lap l:db.laps(x.id)){XSSFRow row=la.createRow(r++);row.createCell(0).setCellValue(x.id);row.createCell(1).setCellValue(l.number);row.createCell(2).setCellValue(l.seconds);}
  Set<String> used=new HashSet<>();
  for(Track t:ts){String base=t.name==null?"Circuito":t.name.trim();if(base.length()==0)base="Circuito";String n=base.substring(0,Math.min(31,base.length()));String candidate=n;int k=2;while(used.contains(candidate)||candidate.equals("Resumen")||candidate.equals("Pilotos")||candidate.equals("Coches")||candidate.equals("Circuitos")||candidate.equals("Sesiones")||candidate.equals("Vueltas")){String suf=" "+k++;candidate=n.substring(0,Math.min(31-suf.length(),n.length()))+suf;}used.add(candidate);XSSFSheet sh=wb.createSheet(candidate);String[] h={"Pos","Piloto","Coche","Mejor tiempo (s)"};for(int i=0;i<h.length;i++)sh.createRow(0).createCell(i).setCellValue(h[i]);int pos=1;for(Object[] z:db.recordRows(t.id,null)){XSSFRow row=sh.createRow(pos);row.createCell(0).setCellValue(pos);Pilot p=pm.get((Long)z[0]);Car c=cm.get((Long)z[1]);row.createCell(1).setCellValue(p==null?"":p.label());row.createCell(2).setCellValue(c==null?"":c.label());row.createCell(3).setCellValue((Double)z[2]);pos++;}}
  FileOutputStream fos=new FileOutputStream(ctx.getCacheDir()+"/CronoSlot.xlsx");wb.write(fos);fos.close();
  OutputStream os=ctx.getContentResolver().openOutputStream(uri);try(InputStream in=new FileInputStream(ctx.getCacheDir()+"/CronoSlot.xlsx")){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)os.write(buf,0,n);}os.close();wb.close();
  android.widget.Toast.makeText(ctx,"Excel generado. Puedes elegir Google Drive en el selector.",android.widget.Toast.LENGTH_LONG).show();
 }
}
