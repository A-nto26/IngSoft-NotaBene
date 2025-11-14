package com.sweng.notes.repository;

import com.sweng.notes.model.Utente;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserRepositoryTest {

    @Test
    void testSaveAndFind() {
        UserRepository repo = new UserRepository();
        Utente u = new Utente("mario", "1234");

        repo.save(u);

        Utente found = repo.find("mario");

        assertNotNull(found);
        assertEquals("mario", found.getUsername());
    }

    @Test
    void testExists() {
        UserRepository repo = new UserRepository();
        repo.save(new Utente("anna", "pass"));

        assertTrue(repo.exists("anna"));
        assertFalse(repo.exists("xxx"));
    }
}
