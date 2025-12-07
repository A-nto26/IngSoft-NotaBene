package com.sweng.notes.dto;

/**
 * DTO per creare o modificare una cartella.
 * Campi:
 * - nome: obbligatorio lato controller/service
 * - creatore: username normalizzato
 * - colore: opzionale, default "#FFD700"
 */
public class FolderRequest {
    private String nome;
    private String creatore;
    private String colore; 

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = (nome == null ? null : nome.trim());
    }

    public String getCreatore() {
        return creatore;
    }

    public void setCreatore(String creatore) {
        this.creatore = (creatore == null ? null : creatore.trim().toLowerCase());
    }

    public String getColore() {
        return colore;
    }

    public void setColore(String colore) {
        if (colore == null || colore.isBlank()) {
            this.colore = "#FFD700";  
        } else {
            this.colore = colore.trim();
        }
    }
}