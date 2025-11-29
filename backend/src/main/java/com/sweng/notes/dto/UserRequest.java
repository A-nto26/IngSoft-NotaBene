package com.sweng.notes.dto;

/**
 * DTO per richieste relative all'utente:
 * - registrazione
 * - login
 *
 * username: normalizzato in lowercase
 * password: mantenuta esattamente come inviata (tranne trim)
 */
public class UserRequest {

    private String username;
    private String password;

    public UserRequest() {
    }

    public UserRequest(String username, String password) {
        setUsername(username);
        setPassword(password);
    }

    // --- GETTERS / SETTERS ---

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
