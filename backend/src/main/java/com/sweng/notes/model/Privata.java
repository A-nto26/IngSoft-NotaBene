package com.sweng.notes.model;

import java.io.Serial;

/**
 * Permesso "Privata":
 * - solo il creatore può leggere e scrivere la nota
 * - gli utenti condivisi non hanno alcun permesso
 */
public class Privata extends Permesso {

    @Serial
    private static final long serialVersionUID = 1L;

    public Privata() {
        super("Privata");
    }

    @Override
    public boolean puoLeggere() {
        return false; // gli utenti condivisi non possono leggere
    }

    @Override
    public boolean puoScrivere() {
        return false; // gli utenti condivisi non possono scrivere
    }
}
