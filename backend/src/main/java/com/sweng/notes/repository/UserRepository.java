package com.sweng.notes.repository;

import com.sweng.notes.logging.LoggerActions;
import com.sweng.notes.model.Utente;
import org.mapdb.*;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Repository MapDB per la gestione degli utenti.
 * 
 * Responsabilità:
 * - Salvataggio e aggiornamento utenti
 * - Normalizzazione case-insensitive dello username
 * - Query per username e snapshot di tutti gli utenti
 * - Supporto a DB reale (file) e DB in-memory per i test
 *
 */
@Repository
public class UserRepository {

    private final DB db;
    private final ConcurrentMap<String, Utente> utenti;

    @SuppressWarnings("unchecked")
    public UserRepository() {

        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        File dbFile = new File(dataDir, "users.db");

        db = DBMaker.fileDB(dbFile)
                .transactionEnable()
                .closeOnJvmShutdown()
                .make();

        utenti = db.hashMap("users", Serializer.STRING, Serializer.JAVA)
                .createOrOpen();
    }

    /**
     * Costruttore alternativo per i test.
     * Utilizza una mappa in-memory invece di un file fisico,
     * permettendo test unitari più veloci e senza effetti collaterali.
     */
    public UserRepository(DB db, Map<String, Utente> usersMap) {
        this.db = db;
        this.utenti = new ConcurrentHashMap<>(usersMap);

    }

    // ============================================================
    // UTILITY - NORMALIZZAZIONE E COMMIT
    // ============================================================
    private String normalize(String username) {
        return (username == null) ? null : username.trim().toLowerCase();
    }

    private void commit() {
        try {
            db.commit();
        } catch (Exception e) {
            LoggerActions.log("USER_DB_COMMIT_FAIL", "system", Map.of("error", e.getMessage()));
            throw e;
        }
    }

    // ============================================================
    // SAVE / UPDATE
    // Salva o aggiorna un utente.
    // Normalizza lo username (lowercase)
    // Impedisce inserimento di valori null o vuoti
    // Commit immediato per garantire consistenza
    // ============================================================
    public synchronized void save(Utente utente) {

        if (utente == null)
            throw new IllegalArgumentException("Utente nullo non consentito");

        if (utente.getUsername() == null || utente.getUsername().isBlank())
            throw new IllegalArgumentException("Username obbligatorio");

        if (utente.getPasswordHash() == null || utente.getPasswordHash().isBlank())
            throw new IllegalArgumentException("Hash della password obbligatorio");

        // Normalizzazione forzata per coerenza del DB
        String norm = normalize(utente.getUsername());
        utente.setUsername(norm);

        utenti.put(norm, utente);
        commit();
    }

    // ============================================================
    // LETTURA - RICERC PER USERNAME
    // ============================================================
    public Utente findByUsername(String username) {
        String norm = normalize(username);
        if (norm == null) {
            return null;
        }
        return utenti.get(norm);
    }

    public boolean exists(String username) {
        String norm = normalize(username);
        return norm != null && utenti.containsKey(norm);
    }

    public Collection<Utente> findAll() {
        return List.copyOf(utenti.values());
    }

    // ============================================================
    // CHIUSURA SICURA DEL DB
    // ============================================================
    public void close() {
        db.close();
    }
}