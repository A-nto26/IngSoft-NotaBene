package com.sweng.notes.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO per aggiungere nuovi utenti alla condivisione di una nota.
 *
 * Regole Sprint 5:
 * - Si possono SOLO aggiungere utenti.
 * - Non è possibile rimuoverli tramite questo DTO.
 * - Il permesso della nota NON viene modificato tramite questo DTO.
 */
public class ShareNoteRequest {

    private List<String> utentiCondivisi;

    public List<String> getUtentiCondivisi() {
        return utentiCondivisi;
    }

    public void setUtentiCondivisi(List<String> utentiCondivisi) {
        if (utentiCondivisi == null) {
            this.utentiCondivisi = List.of();
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
