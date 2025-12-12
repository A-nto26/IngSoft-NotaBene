package com.sweng.notes.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Modello completo della Nota.
 * Include:
 * - Condivisione e permessi (impostati in creazione)
 * - Versionamento
 * - Lock concorrente
 * - Metadati (autore, timestamps)
 * - Colore cartella
 */
public class Note implements Serializable {

    private static final long serialVersionUID = 1L;

    // Identificatore univoco
    private int id;
    // Contenuto
    private String titolo;
    private String contenuto;
    // Proprietario della nota
    private String creatore;
    // Cartella associata
    private String cartella;

    // ===== Condivisione =====
    private Set<String> utentiCondivisi;

    // ===== Versionamento =====
    private List<VersioneNota> versioni;

    // ===== Permessi =====
    private Permesso permesso;

    // ===== Metadati =====
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
    private String lastModifiedBy;

    // ===== Lock concorrente =====
    private String lockedBy;
    private LocalDateTime lockedAt;

    // ===== Colore cartella =====
    private String coloreCartella;

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

        this.coloreCartella = "#ffb347";
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

        this.coloreCartella = "#ffb347";
    }

    public Note(String titolo, String contenuto, String cartella) {
        this(0, titolo, contenuto, null, cartella);
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

    public String getColoreCartella() {
        return coloreCartella;
    }

    public void setColoreCartella(String coloreCartella) {
        this.coloreCartella = coloreCartella;
    }

    // ============================================================
    // VERSIONAMENTO
    // ============================================================

    /** Salva una versione precedente PRIMA della modifica */
    public boolean salvaVersionePrecedente() {

        if ((this.titolo == null || this.titolo.isBlank()) &&
                (this.contenuto == null || this.contenuto.isBlank())) {
            return true; 
        }

        if (this.versioni == null) {
            this.versioni = new ArrayList<>();
        }

        final int MAX_VERSIONI = 50;

        // LIMITE RAGGIUNTO
        if (this.versioni.size() >= MAX_VERSIONI) {
            return false; // segnale: NON salvata
        }

        VersioneNota v = new VersioneNota(
                this.titolo,
                this.contenuto,
                LocalDateTime.now());

        this.versioni.add(0, v);

        return true; // tutto ok
    }

    // ============================================================
    // PERMESSI
    // ============================================================

    public boolean puoLeggere(String username) {
        if (username == null)
            return false;
        if (creatore != null && username.equalsIgnoreCase(creatore))
            return true;

        return utentiCondivisi != null
                && utentiCondivisi.contains(username)
                && permesso != null
                && permesso.puoLeggere();
    }

    public boolean puoScrivere(String username) {
        if (username == null)
            return false;
        if (creatore != null && username.equalsIgnoreCase(creatore))
            return true;

        return utentiCondivisi != null
                && utentiCondivisi.contains(username)
                && permesso != null
                && permesso.puoScrivere();
    }

    // ============================================================
    // METODI DI SUPPORTO PER LOCK
    // ============================================================

    /** True se esiste un lock attivo (lockedBy + lockedAt non null) */
    public boolean hasActiveLock() {
        return lockedBy != null && lockedAt != null;
    }

    /** True se la nota è lockata proprio da questo utente */
    public boolean isLockedBy(String username) {
        if (username == null)
            return false;
        return lockedBy != null && lockedBy.equalsIgnoreCase(username);
    }

    /** True se il lock è scaduto rispetto al timeout */
    public boolean isLockExpired(long timeoutMinutes) {
        if (lockedAt == null)
            return true;
        return lockedAt.plusMinutes(timeoutMinutes).isBefore(LocalDateTime.now());
    }

    /** Imposta il lock a questo utente e aggiorna lockedAt */
    public void acquireLock(String username) {
        this.lockedBy = username;
        this.lockedAt = LocalDateTime.now();
    }

    /** Cancella completamente il lock */
    public void clearLock() {
        this.lockedBy = null;
        this.lockedAt = null;
    }

    @Override
    public String toString() {
        return "Note{id=" + id +
                ", titolo='" + titolo + '\'' +
                ", cartella='" + cartella + '\'' +
                ", creatore='" + creatore + '\'' +
                ", permesso='" + (permesso != null ? permesso.getTipo() : "N/A") + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
