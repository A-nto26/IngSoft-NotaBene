package com.sweng.notes.dto;

import com.sweng.notes.model.Cartella;

/**
 * DTO di risposta per la rappresentazione delle cartelle lato frontend.
 * Contiene:
 * - nome (normalizzato)
 * - colore associato
 * - creatore (username normalizzato)
 *
 *
 */
public class FolderResponse {

    private final String nome;
    private final String colore;
    private final String creatore;

    /** Costruttore vuoto richiesto da Jackson */
    public FolderResponse() {
        this.nome = null;
        this.colore = null;
        this.creatore = null;
    }

    /** Costruttore completo */
    public FolderResponse(String nome, String colore, String creatore) {
        this.nome = nome != null ? nome.trim() : null;
        this.colore = colore != null ? colore.trim() : null;
        this.creatore = creatore != null ? creatore.trim().toLowerCase() : null;
    }

    public static FolderResponse fromCartella(Cartella c) {
        if (c == null) {
            return new FolderResponse(null, null, null);
        }
        return new FolderResponse(
                c.getNome(),
                c.getColore(),
                c.getCreatore());
    }

    // ============================================================
    // GETTER
    // ============================================================

    public String getNome() {
        return nome;
    }

    public String getColore() {
        return colore;
    }

    public String getCreatore() {
        return creatore;
    }
}
