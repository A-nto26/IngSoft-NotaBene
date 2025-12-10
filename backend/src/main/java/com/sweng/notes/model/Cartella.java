package com.sweng.notes.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Modello dati per una cartella persistente.
 * Una cartella contiene:
 *  - nome (univoco, normalizzato dal Repository)
 *  - creatore (utente che l’ha creata)
 *  - colore (usato dal frontend)
 *  - createdAt (timestamp immutabile)
 *  - insieme degli ID delle note contenute
 */
public class Cartella implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String nome;
    private String creatore;
    private String colore;
    private LocalDateTime createdAt;

    /** Insieme degli ID delle note, senza duplicati e ordinati in inserimento */
    private Set<Integer> noteIds = new LinkedHashSet<>();

    // ============================================================
    // COSTRUTTORI
    // ============================================================

    /** Costruttore vuoto richiesto per MapDB */
    public Cartella() {
        this.createdAt = LocalDateTime.now();
        this.colore = "#FFD700";
        this.noteIds = new LinkedHashSet<>();
    }

    public Cartella(String nome, String creatore, String colore) {
        this.nome = (nome != null) ? nome.trim() : "SenzaNome";
        this.creatore = (creatore != null && !creatore.isBlank()) ? creatore.trim() : "system";
        this.colore = (colore != null && !colore.isBlank()) ? colore.trim() : "#FFD700";
        this.createdAt = LocalDateTime.now();
        this.noteIds = new LinkedHashSet<>();
    }

    // ============================================================
    // GETTER / SETTER
    // ============================================================

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        if (nome != null && !nome.isBlank()) {
            this.nome = nome.trim();
        }
    }

    public String getCreatore() {
        return creatore;
    }

    public void setCreatore(String creatore) {
        if (creatore != null && !creatore.isBlank()) {
            this.creatore = creatore.trim();
        }
    }

    public String getColore() {
        return colore;
    }

    public void setColore(String colore) {
        if (colore != null && !colore.isBlank()) {
            this.colore = colore.trim();
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Set<Integer> getNoteIds() {
        return noteIds;
    }

    // ============================================================
    // OPERAZIONI SUL CONTENUTO DELLA CARTELLA 
    // ============================================================

    /** Aggiunge una nota alla cartella */
    public void addNoteId(int id) {
        noteIds.add(id);
    }

    /** Rimuove una nota dalla cartella */
    public void removeNoteId(int id) {
        noteIds.remove(id);
    }

    @Override
    public String toString() {
        return "Cartella{" +
                "nome='" + nome + '\'' +
                ", creatore='" + creatore + '\'' +
                ", colore='" + colore + '\'' +
                ", createdAt=" + createdAt +
                ", noteIds=" + noteIds +
                '}';
    }
}
