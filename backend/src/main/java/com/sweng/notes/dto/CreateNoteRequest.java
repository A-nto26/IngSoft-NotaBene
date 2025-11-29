package com.sweng.notes.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO per la creazione di una nuova nota (Sprint 3).
 * Campi:
 * - titolo, contenuto: testo della nota
 * - creatore: obbligatorio (username dell'autore)
 * - cartella: opzionale
 * - permesso: PRIVATA | LETTURA | SCRITTURA
 * - utentiCondivisi: opzionale, solo aggiunte iniziali
 */
public class CreateNoteRequest {

    private String titolo;
    private String contenuto;
    private String creatore; // obbligatorio
    private String cartella; // opzionale
    private String permesso; // PRIVATA | LETTURA | SCRITTURA
    private List<String> utentiCondivisi; // opzionale

    // ===== GETTER & SETTER =====

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

    public String getCreatore() {
        return creatore;
    }

    public void setCreatore(String creatore) {
        // normalizziamo per sicurezza
        this.creatore = creatore != null ? creatore.trim().toLowerCase() : null;
    }

    public String getCartella() {
        return cartella;
    }

    public void setCartella(String cartella) {
        this.cartella = cartella;
    }

    public String getPermesso() {
        return permesso;
    }

    public void setPermesso(String permesso) {
        // normalizziamo tutto maiuscolo
        this.permesso = (permesso != null) ? permesso.trim().toUpperCase() : null;
    }

    public List<String> getUtentiCondivisi() {
        return utentiCondivisi;
    }

    public void setUtentiCondivisi(List<String> utentiCondivisi) {
        if (utentiCondivisi == null) {
            this.utentiCondivisi = null;
            return;
        }

        List<String> norm = new ArrayList<>();
        for (String u : utentiCondivisi) {
            if (u != null && !u.isBlank()) {
                norm.add(u.trim().toLowerCase());
            }
        }
        this.utentiCondivisi = norm;
    }
}
