package com.sweng.notes.repository;

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
 * Repository MapDB per la gestione degli utenti (Sprint 4).
 * - Username normalizzati (trim + lowercase)
 * - Persistenza sicura in /data/users.db
 * - Commit atomici
 * - Costruttore in-memory per i test
 * - Nessun valore null inserito nel DB
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
     * Costruttore IN-MEMORY per i test, evita cast non sicuri.
     */
    public UserRepository(DB db, Map<String, Utente> usersMap) {
        this.db = db;
        this.utenti = new ConcurrentHashMap<>(usersMap);
        this.db.commit();
    }

    // ============================================================
    // UTILITY
    // ============================================================

    /** Normalizza lo username rendendolo case-insensitive. */
    private String normalize(String username) {
        return (username == null) ? null : username.trim().toLowerCase();
    }

    private void commit() {
        db.commit();
    }

    // ============================================================
    // SAVE / UPDATE
    // ============================================================
    /**
     * Salva o aggiorna un utente nel database.
     * Lo username viene SEMPRE normalizzato.
     */
    public synchronized void save(Utente utente) {

        if (utente == null)
            throw new IllegalArgumentException("Utente nullo non consentito");

        if (utente.getUsername() == null || utente.getUsername().isBlank())
            throw new IllegalArgumentException("Username obbligatorio");

        if (utente.getPasswordHash() == null || utente.getPasswordHash().isBlank())
            throw new IllegalArgumentException("Hash della password obbligatorio");

        String norm = normalize(utente.getUsername());
        utente.setUsername(norm);

        utenti.put(norm, utente);
        commit();
    }

    // ============================================================
    // FIND by username
    // ============================================================
    public Utente findByUsername(String username) {
        String norm = normalize(username);
        if (norm == null) {
            return null;
        }
        return utenti.get(norm);
    }

    // ============================================================
    // EXISTS
    // ============================================================
    public boolean exists(String username) {
        String norm = normalize(username);
        return norm != null && utenti.containsKey(norm);
    }

    // ============================================================
    // FIND ALL — snapshot immutabile
    // ============================================================
    public Collection<Utente> findAll() {
        return List.copyOf(utenti.values()); // snapshot & immutabile
    }

    // ============================================================
    // CLOSE DB
    // ============================================================
    public void close() {
        db.close();
    }
}