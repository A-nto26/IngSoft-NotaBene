package com.sweng.notes.dto;

import java.util.List;

/**
 * DTO per aggiornare una nota esistente.
 *
 * Campi modificabili:
 * - titolo
 * - contenuto
 * - cartella
 * - coloreCartella
 *
 * utentiCondivisi:
 *   Ignorato nello /update — non modifica la condivisione.
 *   La gestione reale avviene SOLO tramite /share e /removeSelf.
 *
 * versionExpected:
 *   indica la versione che il frontend si aspetta di modificare.
 *   Serve per prevenire conflitti di modifica concorrente (Sprint 4, L3-L4).
 */
public class NoteUpdateRequest {

    private String titolo;
    private String contenuto;
    private String cartella;

    // Ignorato dall'update: rimane per compatibilità frontend
    private List<String> utentiCondivisi;

    private String coloreCartella;

    // Versione attesa dal frontend per il controllo di consistenza
    private Integer versionExpected;

    private String permesso;
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

    public String getPermesso() {
    return permesso;
    }

    public void setPermesso(String permesso) {
        this.permesso = permesso;
     }
}
