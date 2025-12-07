package com.sweng.notes.dto;

import java.util.List;

/**
 * DTO per aggiungere utenti alla condivisione di una nota esistente.
 * È possibile solo aggiungere utenti, non rimuoverli.
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