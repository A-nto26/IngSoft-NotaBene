package com.sweng.notes.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Rappresenta una versione precedente di una nota.
 * Ogni versione contiene:
 * - titolo
 * - contenuto
 * - timestamp del momento in cui la versione è stata salvata
 */
public class VersioneNota implements Serializable {

    private static final long serialVersionUID = 1L;

    private String titolo;
    private String contenuto;

    /**
     * Momento del salvataggio della versione.
     * Non è il createdAt della nota originale, ma il momento della snapshot.
     */
    private LocalDateTime timestamp;

    /** Costruttore vuoto richiesto per MapDB */
    public VersioneNota() {
        this.timestamp = LocalDateTime.now();
    }

    public VersioneNota(String titolo, String contenuto, LocalDateTime timestamp) {
        this.titolo = titolo;
        this.contenuto = contenuto;
        this.timestamp = timestamp;
    }

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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
