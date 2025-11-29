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
        this.nome = (nome != null && !nome.isBlank())
                ? nome.trim()
                : null;
    }

    public String getCreatore() {
        return creatore;
    }

    public void setCreatore(String creatore) {
        this.creatore = (creatore != null && !creatore.isBlank())
                ? creatore.trim().toLowerCase()
                : null;
    }

    public String getColore() {
        return colore;
    }

    public void setColore(String colore) {
        if (colore == null || colore.isBlank()) {
            this.colore = "#FFD700"; // default coerente con Cartella
        } else {
            this.colore = colore.trim();
        }
    }
}
