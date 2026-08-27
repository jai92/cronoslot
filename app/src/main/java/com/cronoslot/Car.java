package com.cronoslot;

public class Car {
    public long id;
    public String name, brand, model, chassis, frontTyre, rearTyre, braid, notes, photo;

    public Car(long id, String name, String brand, String model, String chassis,
               String frontTyre, String rearTyre, String braid, String notes, String photo) {
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.model = model;
        this.chassis = chassis;
        this.frontTyre = frontTyre;
        this.rearTyre = rearTyre;
        this.braid = braid;
        this.notes = notes;
        this.photo = photo;
    }

    public String label() {
        if (brand == null || brand.trim().isEmpty()) return name == null ? "" : name;
        if (model == null || model.trim().isEmpty()) return (name == null ? "" : name) + " · " + brand;
        return (name == null ? "" : name) + " · " + brand + " " + model;
    }
}
