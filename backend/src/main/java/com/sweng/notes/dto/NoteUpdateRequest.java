package com.sweng.notes.dto;

import java.util.List;

/**
 * DTO per aggiornare una nota esistente.
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
    private List<String> utentiCondivisi;
    private String coloreCartella;

    // Versione attesa dal frontend
    private Integer versionExpected;

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

    public List<String> getUtentiCondivisi() {
        return utentiCondivisi;
    }

    public void setUtentiCondivisi(List<String> utentiCondivisi) {
        this.utentiCondivisi = utentiCondivisi;
    }

    public String getColoreCartella() {
        return coloreCartella;
    }

    public void setColoreCartella(String coloreCartella) {
        this.coloreCartella = coloreCartella;
    }

    public Integer getVersionExpected() {
        return versionExpected;
    }

    public void setVersionExpected(Integer versionExpected) {
        this.versionExpected = versionExpected;
    }
}