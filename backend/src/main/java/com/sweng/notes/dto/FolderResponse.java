package com.sweng.notes.dto;

/**
 * DTO di risposta per la rappresentazione delle cartelle lato frontend.
 * Contiene:
 * - nome della cartella
 * - colore associato
 * - creatore (username)
 *
 * Viene usato dal frontend per popolare la lista delle cartelle.
 * Nessuna logica applicativa: solo trasporto dati.
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
