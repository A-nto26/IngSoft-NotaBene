package com.sweng.notes.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * Classe astratta che rappresenta il tipo di permesso associato a una nota.
 *
 * Implementazioni disponibili:
 *  - Privata      → solo autore può leggere e scrivere
 *  - Lettura      → autore + utenti condivisi possono leggere
 *  - Scrittura    → autore + utenti condivisi possono leggere e scrivere
 *
 * Il permesso viene assegnato esclusivamente in fase di creazione della nota
 * e non può essere modificato successivamente.
 */
public abstract class Permesso implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Nome del permesso (es. "Privata", "Lettura", "Scrittura") */
    private final String tipo;

    protected Permesso(String tipo) {
        this.tipo = tipo;
    }

    /** Ritorna il nome del permesso. */
    public String getTipo() {
        return tipo;
    }

    /** Indica se l'utente condiviso può leggere la nota */
    public abstract boolean puoLeggere();

    /** Indica se l'utente condiviso può scrivere/modificare la nota */
    public abstract boolean puoScrivere();

    @Override
    public String toString() {
        return tipo;
    }
}
