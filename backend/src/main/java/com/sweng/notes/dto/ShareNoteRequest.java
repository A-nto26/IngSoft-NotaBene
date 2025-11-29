package com.sweng.notes.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO per aggiungere utenti alla condivisione di una nota esistente.
 * Sprint 3: è possibile solo aggiungere utenti, non rimuoverli.
 */
public class ShareNoteRequest {

    private List<String> utentiCondivisi;

    public List<String> getUtentiCondivisi() {
        return utentiCondivisi;
    }

    public void setUtentiCondivisi(List<String> utentiCondivisi) {

        if (utentiCondivisi == null) {
            this.utentiCondivisi = null;
            return;
        }

        List<String> norm = new ArrayList<>();

        for (String u : utentiCondivisi) {
            if (u != null && !u.isBlank()) {
                norm.add(u.trim().toLowerCase());
            }
        }

        this.utentiCondivisi = norm;
    }
}
