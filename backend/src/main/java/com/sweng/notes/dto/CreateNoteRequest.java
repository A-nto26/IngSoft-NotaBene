package com.sweng.notes.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO utilizzato per la creazione di una nuova nota.
 *
 * Contiene esclusivamente i dati inviati dal frontend:
 * - titolo, contenuto: testo della nota
 * - creatore: username dell’autore (obbligatorio)
 * - cartella: nome della cartella scelta (opzionale)
 * - permesso: PRIVATA | LETTURA | SCRITTURA (impostato solo in creazione)
 * - utentiCondivisi: elenco opzionale di utenti inizialmente aggiunti
 * - coloreCartella: colore selezionato dal frontend (opzionale)
 */
public class CreateNoteRequest {

    private String titolo;
    private String contenuto;
    private String creatore;
    private String cartella;
    private String permesso;
    private List<String> utentiCondivisi = new ArrayList<>();
    private String coloreCartella;

    // ===========================================
    // GETTER & SETTER
    // ===========================================
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
        this.creatore = creatore;
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
        this.permesso = permesso;
    }

    public List<String> getUtentiCondivisi() {
        return utentiCondivisi;
    }

    public void setUtentiCondivisi(List<String> utentiCondivisi) {
        this.utentiCondivisi = (utentiCondivisi != null) ? utentiCondivisi : new ArrayList<>();
    }

    public String getColoreCartella() {
        return coloreCartella;
    }

    public void setColoreCartella(String coloreCartella) {
        this.coloreCartella = coloreCartella;
    }

}
