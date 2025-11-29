package com.sweng.notes.model;

import java.io.Serial;

/**
 * Permesso "Lettura":
 * - autore + utenti condivisi possono leggere
 * - solo autore può scrivere
 */
public class Lettura extends Permesso {

    @Serial
    private static final long serialVersionUID = 1L;

    public Lettura() {
        super("Lettura");
    }

    @Override
    public boolean puoLeggere() {
        return true;
    }

    @Override
    public boolean puoScrivere() {
        return false; // gli utenti condivisi NON possono modificare
    }
}
