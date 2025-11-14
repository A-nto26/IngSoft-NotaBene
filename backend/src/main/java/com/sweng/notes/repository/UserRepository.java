package com.sweng.notes.repository;

import com.sweng.notes.model.Utente;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class UserRepository {

    private final Map<String, Utente> utenti = new HashMap<>();

    public void save(Utente utente) {
        utenti.put(utente.getUsername(), utente);
    }

    public Utente find(String username) {
        return utenti.get(username);
    }

    public boolean exists(String username) {
        return utenti.containsKey(username);
    }
}
