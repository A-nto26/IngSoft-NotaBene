package com.sweng.notes.repository;

import com.sweng.notes.model.Utente;
import org.junit.jupiter.api.Test;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class UserRepositoryTest {

    @Test
    void testSaveAndFindUser() {
        DB db = DBMaker.memoryDB().make();
        Map<String, Utente> map = new HashMap<>();

        UserRepository repo = new UserRepository(db, map);

        Utente u = new Utente("Mario", "1234");
        repo.save(u);

        Utente found = repo.findByUsername("mario"); // normalizzato minuscolo
        assertNotNull(found);
        assertEquals("mario", found.getUsername().toLowerCase());
    }

    @Test
    void testExists() {
        DB db = DBMaker.memoryDB().make();
        Map<String, Utente> map = new HashMap<>();

        UserRepository repo = new UserRepository(db, map);

        repo.save(new Utente("Anna", "pass"));

        assertTrue(repo.exists("anna"));       // lowercase
        assertTrue(repo.exists("ANNA"));       // uppercase → normalizzato
        assertFalse(repo.exists("xxx"));
    }

    @Test
    void testFindAllUsers() {
        DB db = DBMaker.memoryDB().make();
        Map<String, Utente> map = new HashMap<>();

        UserRepository repo = new UserRepository(db, map);

        repo.save(new Utente("a", "1"));
        repo.save(new Utente("b", "2"));

        assertEquals(2, repo.findAll().size());
    }
}
