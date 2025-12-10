package com.sweng.notes.dto;

import java.util.List;

/**
 * DTO per aggiungere utenti alla condivisione di una nota esistente.
 *
 * Regole :
 * - Si possono SOLO aggiungere utenti.
 * - Il permesso della nota NON viene modificato tramite questo DTO.
 * - La rimozione è effettuata esclusivamente tramite /removeSelf.
 */
public class ShareNoteRequest {

    private List<String> utentiCondivisi;

    public List<String> getUtentiCondivisi() {
        return utentiCondivisi;
    }

    public void setUtentiCondivisi(List<String> utentiCondivisi) {
        this.utentiCondivisi = utentiCondivisi;
    }
}