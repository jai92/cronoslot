package com.cronoslot;
public class Car {
 public long id; public String brand,model,chassis,frontTyre,rearTyre,braid,notes,photo;
 public Car(long id,String brand,String model,String chassis,String frontTyre,String rearTyre,String braid,String notes,String photo){
  this.id=id;this.brand=brand;this.model=model;this.chassis=chassis;this.frontTyre=frontTyre;this.rearTyre=rearTyre;this.braid=braid;this.notes=notes;this.photo=photo;
 }
 public String label(){return brand+" "+model;}
}
