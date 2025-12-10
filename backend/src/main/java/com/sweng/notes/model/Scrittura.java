package com.sweng.notes.model;

import java.io.Serial;

/**
 * Permesso "Scrittura":
 * - autore + utenti condivisi possono leggere e scrivere la nota
 *
 * (L’autore ha sempre pieno accesso; questi flag si applicano agli utenti condivisi)
 */
public class Scrittura extends Permesso {

    @Serial
    private static final long serialVersionUID = 1L;

    public Scrittura() {
        super("Scrittura");
    }

    /** Gli utenti condivisi possono leggere */
    @Override
    public boolean puoLeggere() {
        return true;
    }

    /** Gli utenti condivisi possono anche scrivere */
    @Override
    public boolean puoScrivere() {
        return true;
    }
}
