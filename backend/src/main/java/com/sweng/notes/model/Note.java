package com.sweng.notes.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Modello completo della Nota per Sprint 3:
 * - Condivisione
 * - Permessi (impostati solo in creazione)
 * - Versionamento
 * - Lock concorrente
 * - Metadati (created / modified)
 */
public class Note implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String titolo;
    private String contenuto;
    private String creatore;
    private String cartella;

    // ===== Condivisione =====
    private Set<String> utentiCondivisi;

    // ===== Versionamento =====
    private List<VersioneNota> versioni;

    // ===== Permessi =====
    // Il permesso viene impostato SOLO in creazione (regola Sprint 3)
    private Permesso permesso;

    // ===== Metadati =====
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
    private String lastModifiedBy;

    // ===== Lock concorrente =====
    private String lockedBy;
    private LocalDateTime lockedAt;

    // ============================================================
    // COSTRUTTORI
    // ============================================================

    /** Costruttore vuoto richiesto per MapDB */
    public Note() {
        this.id = 0;
        this.titolo = "";
        this.contenuto = "";
        this.creatore = null;
        this.cartella = null;

        this.utentiCondivisi = new LinkedHashSet<>();
        this.versioni = new ArrayList<>();

        // permesso di fallback: il servizio imposterà quello corretto
        this.permesso = new Privata();

        this.createdAt = LocalDateTime.now();
        this.lastModifiedAt = LocalDateTime.now();
        this.lastModifiedBy = null;

        this.lockedBy = null;
        this.lockedAt = null;
    }

    public Note(int id, String titolo, String contenuto, String creatore, String cartella) {
        this.id = id;
        this.titolo = titolo;
        this.contenuto = contenuto;
        this.creatore = creatore;
        this.cartella = cartella;

        this.utentiCondivisi = new LinkedHashSet<>();
        this.versioni = new ArrayList<>();
        this.permesso = new Privata();

        this.createdAt = LocalDateTime.now();
        this.lastModifiedAt = LocalDateTime.now();
        this.lastModifiedBy = creatore;

        this.lockedBy = null;
        this.lockedAt = null;
    }

    // ============================================================
    // GETTER e SETTER
    // ============================================================

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
        this.lastModifiedAt = LocalDateTime.now();
    }

    public String getContenuto() {
        return contenuto;
    }

    public void setContenuto(String contenuto) {
        this.contenuto = contenuto;
        this.lastModifiedAt = LocalDateTime.now();
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

    public Set<String> getUtentiCondivisi() {
        return utentiCondivisi;
    }

    public void setUtentiCondivisi(Set<String> utentiCondivisi) {
        this.utentiCondivisi = (utentiCondivisi != null)
                ? new LinkedHashSet<>(utentiCondivisi)
                : new LinkedHashSet<>();
    }

    public List<VersioneNota> getVersioni() {
        return versioni;
    }

    public void setVersioni(List<VersioneNota> versioni) {
        this.versioni = (versioni != null)
                ? new ArrayList<>(versioni)
                : new ArrayList<>();
    }

    public Permesso getPermesso() {
        return permesso;
    }

    // permesso impostato solo in creazione (regola Sprint 3)
    public void setPermesso(Permesso permesso) {
        this.permesso = permesso;
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

    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    // ============================================================
    // METODI LOGICI
    // ============================================================

    /** Salva una versione precedente PRIMA della modifica */
    public void salvaVersionePrecedente() {

        if ((this.titolo == null || this.titolo.isBlank()) &&
                (this.contenuto == null || this.contenuto.isBlank())) {
            return;
        }

        if (this.versioni == null) {
            this.versioni = new ArrayList<>();
        }

        VersioneNota v = new VersioneNota(
                this.titolo,
                this.contenuto,
                LocalDateTime.now());

        this.versioni.add(0, v);

        // massimo 20 versioni
        if (this.versioni.size() > 20) {
            this.versioni.remove(this.versioni.size() - 1);
        }
    }

    /** L’utente può leggere? */
    public boolean puoLeggere(String username) {
        if (username == null)
            return false;
        if (creatore != null && username.equalsIgnoreCase(creatore))
            return true;

        return utentiCondivisi.contains(username)
                && permesso != null
                && permesso.puoLeggere();
    }

    /** L’utente può scrivere? */
    public boolean puoScrivere(String username) {
        if (username == null)
            return false;
        if (creatore != null && username.equalsIgnoreCase(creatore))
            return true;

        return utentiCondivisi.contains(username)
                && permesso != null
                && permesso.puoScrivere();
    }

    @Override
    public String toString() {
        return "Note{id=" + id +
                ", titolo='" + titolo + '\'' +
                ", creatore='" + creatore + '\'' +
                ", permesso='" + (permesso != null ? permesso.getTipo() : "N/A") + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
