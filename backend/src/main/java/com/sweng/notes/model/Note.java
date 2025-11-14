package com.sweng.notes.model;

public class Note {

    private int id;
    private String titolo;
    private String contenuto;
    private String creatore;

    public Note(int id, String titolo, String contenuto, String creatore) {
        this.id = id;
        this.titolo = titolo;
        this.contenuto = contenuto;
        this.creatore = creatore;
    }

    public int getId() {
        return id;
    }

    public String getTitolo() {
        return titolo;
    }

    public String getContenuto() {
        return contenuto;
    }

    public String getCreatore() {
        return creatore;
    }
}
