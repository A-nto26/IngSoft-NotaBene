package com.sweng.notes.model;

import java.io.Serial;

/**
 * Permesso "Scrittura":
 * - autore + utenti condivisi possono leggere e scrivere
 */
public class Scrittura extends Permesso {

    @Serial
    private static final long serialVersionUID = 1L;

    public Scrittura() {
        super("Scrittura");
    }

    @Override
    public boolean puoLeggere() {
        return true;
    }

    @Override
    public boolean puoScrivere() {
        return true;
    }
}
