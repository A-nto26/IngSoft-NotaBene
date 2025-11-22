package com.sweng.notes.model;

import java.time.LocalDateTime;

public class Note {

    private int id;
    private String titolo;
    private String contenuto;
    private String creatore;
    private String cartella;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;

    public Note(int id, String titolo, String contenuto, String creatore, String cartella) {
        this.id = id;
        this.titolo = titolo;
        this.contenuto = contenuto;
        this.creatore = creatore;
        this.cartella = cartella;
        this.createdAt = LocalDateTime.now();
        this.lastModifiedAt = LocalDateTime.now();
    }

    // GETTER
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

    public String getCartella() {
        return cartella;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    // SETTER
    public void setId(int id) {
        this.id = id;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public void setContenuto(String contenuto) {
        this.contenuto = contenuto;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public void setCartella(String cartella) {
        this.cartella = cartella;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public void setLastModifiedAt(LocalDateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }
}