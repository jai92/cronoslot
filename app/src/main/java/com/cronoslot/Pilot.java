package com.cronoslot;
import java.util.*;
public class Pilot {
 public long id; public String name,surname,photo; public List<String> remotes;
 public Pilot(long id,String name,String surname,List<String> remotes,String photo){this.id=id;this.name=name;this.surname=surname;this.remotes=remotes;this.photo=photo;}
 public String label(){return name+" "+surname;}
}
