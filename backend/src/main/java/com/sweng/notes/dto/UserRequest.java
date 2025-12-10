package com.sweng.notes.dto;

/**
 * DTO utilizzato per:
 * - registrazione utente
 * - login
 *
 * Regole:
 * - username: normalizzato in lowercase (case-insensitive)
 * - password: mantenuta così com'è, salvo trim (NO lowercase)
 */
public class UserRequest {

    private String username;
    private String password;

    /** Costruttore vuoto richiesto da Jackson */
    public UserRequest() {
    }

    /** Costruttore completo */
    public UserRequest(String username, String password) {
        setUsername(username);
        setPassword(password);
    }

    // ============================================================
    // GETTER / SETTER
    // ============================================================

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = (username != null && !username.isBlank())
                ? username.trim().toLowerCase()
                : null;
    }

    public String getPassword() {
        return password;
    }

    /** 
     * La password non viene modificata (NO lowercase), solo trim.
     * L'hashing è gestito esclusivamente da UserService.
     */
    public void setPassword(String password) {
        this.password = (password != null) ? password.trim() : null;
    }
}
