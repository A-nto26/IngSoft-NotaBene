package com.sweng.notes.dto;

/**
 * DTO usato dal frontend per richiedere il cambio permesso di una nota.
 *
 * Valori ammessi:
 * - "privata"
 * - "lettura"
 * - "scrittura"
 *
 */
public class PermessoRequest {

    private String permesso;

    public String getPermesso() {
        return permesso;
    }

    public void setPermesso(String permesso) {
        this.permesso = (permesso == null ? null : permesso.trim().toLowerCase());
    }
}
