package com.sweng.notes.model;

import java.io.Serializable;

/**
 * Modello utente semplice e persistente.
 * Regole :
 * - username sempre normalizzato (trim + lowercase)
 * - password salvata solo come hash BCrypt
 * - nessuna logica applicativa: tutto è gestito da UserService
 */
public class Utente implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Username dell'utente, sempre in minuscolo */
    private String username;

    /** Hash della password (BCrypt). Mai in chiaro. */
    private String passwordHash;

    /** Costruttore vuoto richiesto da MapDB */
    public Utente() {
    }

    /** Costruttore principale */
    public Utente(String username, String passwordHash) {
        this.username = (username != null)
                ? username.trim().toLowerCase()
                : null;

        this.passwordHash = (passwordHash != null && !passwordHash.isBlank())
                ? passwordHash
                : null;
    }

    // ============================================================
    // GETTER / SETTER
    // ============================================================

    public String getUsername() {
        return username;
    }

    /** Lo username viene sempre salvato normalizzato */
    public void setUsername(String username) {
        this.username = (username != null)
                ? username.trim().toLowerCase()
                : null;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Imposta l'hash della password.
     * (UserService garantisce che sia sempre un hash BCrypt valido.)
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = (passwordHash != null && !passwordHash.isBlank())
                ? passwordHash
                : null;
    }

    @Override
    public String toString() {
        return " ✅ Utente{ username='" + username + "' }";
    }
}