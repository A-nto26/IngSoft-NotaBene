package com.sweng.notes.model;

import java.io.Serial;

/**
 * Permesso "Sola Lettura":
 * Regole:
 *  - Tutti gli utenti con cui la nota è condivisa possono leggere.
 *  - Solo l’autore può scrivere.
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
        return false;
    }
}
