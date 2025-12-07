package com.sweng.notes.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * Modello astratto per rappresentare il tipo di permesso associato a una nota.
 *
 * Le tre implementazioni previste per Sprint 3 sono:
 * - Privata → solo l’autore può leggere e scrivere
 * - SolaLettura → autore + utenti condivisi possono leggere
 * - Scrittura → autore + utenti condivisi possono leggere e scrivere
 *
 * Il permesso viene impostato SOLO in fase di creazione della nota (regola
 * Sprint 3)
 * e non può essere cambiato successivamente.
 */
public abstract class Permesso implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Nome del permesso (es. "Privata", "Lettura", "Scrittura") */
    private final String tipo;

    protected Permesso(String tipo) {
        this.tipo = tipo;
    }

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
