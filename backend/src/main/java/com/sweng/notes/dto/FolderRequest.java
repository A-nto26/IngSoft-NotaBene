package com.sweng.notes.dto;

/**
 * DTO per la creazione o modifica di una cartella.
 * 
 */
public class FolderRequest {

    private String nome;
    private String creatore;
    private String colore;

    // GETTER & SETTER
    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCreatore() {
        return creatore;
    }

    public void setCreatore(String creatore) {
        this.creatore = creatore;
    }

    public String getColore() {
        return colore;
    }

    public void setColore(String colore) {
        this.colore = colore;
    }
}
