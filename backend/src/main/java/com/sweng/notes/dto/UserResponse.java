package com.sweng.notes.dto;

/**
 * Risposta standard per tutte le operazioni sugli utenti.
 *
 * Campi:
 * - success: esito booleano dell'operazione
 * - message: messaggio leggibile lato frontend
 * - username: opzionale, sempre normalizzato in lowercase
 */
public class UserResponse {

    private boolean success;
    private String message;
    private String username;

    /** Costruttore vuoto richiesto per Jackson */
    public UserResponse() {
    }

    /** Risposta senza username (es. errori) */
    public UserResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    /** Risposta completa (es. login riuscito) */
    public UserResponse(boolean success, String message, String username) {
        this.success = success;
        this.message = message;
        setUsername(username);
    }

    // ============================================================
    // GETTER / SETTER
    // ============================================================

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

    /**
     * L'username viene sempre normalizzato in lowercase,
     * in modo coerente con UserRequest e UserService.
     */
    public void setUsername(String username) {
        this.username = (username != null && !username.isBlank())
                ? username.trim().toLowerCase()
                : null;
    }
}
