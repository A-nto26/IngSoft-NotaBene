package com.sweng.notes.dto;

public class NoteUpdateRequest {

    private String titolo;
    private String contenuto;

    public String getTitolo() { 
        return titolo; 
    }
    public void setTitolo(String titolo) { 
        this.titolo = titolo; 
    }

    public String getContenuto() { 
        return contenuto; 
    }
    public void setContenuto(String contenuto) { 
        this.contenuto = contenuto;
        }
}