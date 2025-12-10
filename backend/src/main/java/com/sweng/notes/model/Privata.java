package com.sweng.notes.model;

import java.io.Serial;

/**
 * Permesso "Privata":
 * - solo il creatore può leggere e scrivere la nota
 * - gli utenti condivisi NON hanno alcun permesso
 *
 * (L'autore ha sempre tutti i diritti: la logica è gestita in Note.puoLeggere / puoScrivere)
 */
public class Privata extends Permesso {

    @Serial
    private static final long serialVersionUID = 1L;

    public Privata() {
        super("Privata");
    }

    /** Gli utenti condivisi non possono leggere */
    @Override
    public boolean puoLeggere() {
        return false; 
    }

    /** Gli utenti condivisi non possono scrivere */
    @Override
    public boolean puoScrivere() {
        return false; 
    }
}
