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
 * Repository MapDB per gli utenti registrati.
 * Mantiene uno storage semplice, case-insensitive e thread-safe.
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
    // SAVE / UPDATE
    // ============================================================
    public synchronized void save(Utente utente) {
        if (utente == null || utente.getUsername() == null)
            return;

        utenti.put(
                utente.getUsername().trim().toLowerCase(),
                utente);

        db.commit();
    }

    // ============================================================
    // FIND by username (Sprint 3 naming)
    // ============================================================
    public Utente findByUsername(String username) {
        if (username == null)
            return null;

        return utenti.get(username.trim().toLowerCase());
    }

    // ============================================================
    // EXISTS
    // ============================================================
    public boolean exists(String username) {
        if (username == null)
            return false;

        return utenti.containsKey(username.trim().toLowerCase());
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
