package com.cronoslot;

public class Track {
    public long id;
    public String name;
    public double length;
    public double minLap;
    public String notes, photo;

    public Track(long id, String name, double length, double minLap, String notes, String photo) {
        this.id = id;
        this.name = name == null ? "" : name;
        this.length = length;
        this.minLap = minLap;
        this.notes = notes == null ? "" : notes;
        this.photo = photo;
    }
}
