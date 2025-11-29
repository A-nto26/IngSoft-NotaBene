package com.sweng.notes.dto;

/**
 * Risposta standard per operazioni relative agli utenti:
 * - success: indica esito booleano
 * - message: messaggio leggibile dal frontend
 * - username: opzionale, sempre normalizzato in lowercase
 */
public class UserResponse {

    private boolean success;
    private String message;
    private String username;

    public UserResponse() {
    }

    public UserResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public UserResponse(boolean success, String message, String username) {
        this.success = success;
        this.message = message;
        setUsername(username);
    }

    // --- GETTERS / SETTERS ---

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = (username != null && !username.isBlank())
                ? username.trim().toLowerCase()
                : null;
    }
}
