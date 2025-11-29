package com.sweng.notes.dto;

/**
 * DTO per aggiornare una nota esistente (Sprint 3).
 * Campi modificabili:
 * - titolo
 * - contenuto
 * - cartella
 *
 * Non è possibile modificare:
 * - creatore
 * - permesso
 * - utenti condivisi (solo via /share)
 */
public class NoteUpdateRequest {

    private String titolo;
    private String contenuto;
    private String cartella;

    // ==============================
    // Getter e Setter
    // ==============================

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

    public String getCartella() {
        return cartella;
    }

    public void setCartella(String cartella) {
        this.cartella = cartella;
    }
}
