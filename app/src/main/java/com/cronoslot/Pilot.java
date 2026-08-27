package com.cronoslot;

public class Pilot {
    public long id;
    public String name, surname, remotes, photo;

    public Pilot(long id, String name, String surname, String remotes, String photo) {
        this.id = id;
        this.name = name == null ? "" : name;
        this.surname = surname == null ? "" : surname;
        this.remotes = remotes == null ? "" : remotes;
        this.photo = photo;
    }

    public String label() {
        return (name + " " + surname).trim();
    }
}
