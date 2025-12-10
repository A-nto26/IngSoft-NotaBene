package com.sweng.notes.dto;

/**
 * DTO per la creazione o modifica di una cartella.
 *
 * Campi:
 * - nome: nome della cartella (obbligatorio lato controller/service)
 * - creatore: username dell'autore (normalizzato lato service)
 * - colore: opzionale, sarà il service a gestire il default "#FFD700"
 *
 * Il DTO non applica logica: trasporta esclusivamente i dati inviati dal frontend.
 */
public class FolderRequest {
    private String nome;
    private String creatore;
    private String colore; 

    // ============================================================
    // GETTER & SETTER
    // ============================================================

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