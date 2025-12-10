package com.sweng.notes.dto;

import com.sweng.notes.model.VersioneNota;
import java.time.LocalDateTime;
import java.util.List;

public class NoteView {

    private int id;
    private String titolo;
    private String contenuto;
    private String cartella;
    private String coloreCartella;

    // Permesso: "privata", "lettura", "scrittura"
    private String permesso;

    // Autore della nota
    private String creatore;

    // Utenti con cui è condivisa (escluso autore e utente corrente)
    private List<String> condivisaCon;

    private String ruolo; // autore | lettura | scrittura
    private String lockedBy; // utente che detiene il lock
    private int versione; // numero versione corrente

    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
    private String lastModifiedBy;
    private List<VersioneNota> versioni;

    // ==========================================================
    // GETTER / SETTER
    // ==========================================================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getCartella() {
        return cartella;
    }

    public void setCartella(String cartella) {
        this.cartella = cartella;
    }

    public String getColoreCartella() {
        return coloreCartella;
    }

    public void setColoreCartella(String coloreCartella) {
        this.coloreCartella = coloreCartella;
    }

    public String getPermesso() {
        return permesso;
    }

    public void setPermesso(String permesso) {
        this.permesso = permesso;
    }

    public String getCreatore() {
        return creatore;
    }

    public void setCreatore(String creatore) {
        this.creatore = creatore;
    }

    public List<String> getCondivisaCon() {
        return condivisaCon;
    }

    public void setCondivisaCon(List<String> condivisaCon) {
        this.condivisaCon = condivisaCon;
    }

    public String getRuolo() {
        return ruolo;
    }

    public void setRuolo(String ruolo) {
        this.ruolo = ruolo;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
    }

    public int getVersione() {
        return versione;
    }

    public void setVersione(int versione) {
        this.versione = versione;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(LocalDateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public List<VersioneNota> getVersioni() {
        return versioni;
    }

    public void setVersioni(List<VersioneNota> versioni) {
        this.versioni = versioni;
    }

}
