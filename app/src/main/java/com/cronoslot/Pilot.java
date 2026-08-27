package com.cronoslot;

public class Pilot {
    public long id;
    public String name, surname, remotes, photo;

    public Pilot(long id, String name, String surname, String remotes, String photo) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.remotes = remotes == null ? "" : remotes;
        this.photo = photo;
    }

    public String label() {
        String a = name == null ? "" : name.trim();
        String b = surname == null ? "" : surname.trim();
        return (a + " " + b).trim();
    }
}
