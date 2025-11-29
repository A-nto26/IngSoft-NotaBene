package com.sweng.notes.dto;

/**
 * DTO di risposta contenente informazioni sulle cartelle.
 * Usato nel frontend per mostrare la lista delle cartelle.
 */
public class FolderResponse {

    private String nome;
    private String colore;
    private String creatore;

    /** Costruttore vuoto richiesto per Jackson/serializzazione */
    public FolderResponse() {
    }

    /** Costruttore completo */
    public FolderResponse(String nome, String colore, String creatore) {
        this.nome = nome != null ? nome.trim() : null;
        this.colore = colore != null ? colore.trim() : null;
        this.creatore = creatore != null ? creatore.trim().toLowerCase() : null;
    }

    // ===== Getter =====

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
