package com.sweng.notes.repository;

import com.sweng.notes.model.Utente;
import org.junit.jupiter.api.*;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryTest {

    private DB db;
    private Map<String, Utente> map;
    private UserRepository repo;

    @BeforeEach
    void setup() {
        db = DBMaker.memoryDB()
                .transactionEnable()
                .make();

        map = new HashMap<>();
        repo = new UserRepository(db, map);
    }

    @AfterEach
    void cleanup() {
        repo.close();
    }

    // ============================================================
    // SAVE + FIND
    // ============================================================
    @Test
    void testSaveAndFindUser() {
        Utente u = new Utente("Mario", "HASH");
        repo.save(u);

        Utente found = repo.findByUsername("mario");
        assertNotNull(found);
        assertEquals("mario", found.getUsername());
    }

    @Test
    void testExistsIsCaseInsensitive() {
        repo.save(new Utente("Anna", "HASH"));

        assertTrue(repo.exists("anna"));
        assertTrue(repo.exists("ANNA"));
        assertFalse(repo.exists("xxx"));
    }

    @Test
    void testFindAllUsers() {
        repo.save(new Utente("a", "H1"));
        repo.save(new Utente("b", "H2"));

        Collection<Utente> all = repo.findAll();
        assertEquals(2, all.size());
    }

    // ============================================================
    // VALIDAZIONI MINIME 
    // ============================================================
    @Test
    void testSaveRejectsNullUser() {
        assertThrows(IllegalArgumentException.class, () -> repo.save(null));
    }

    @Test
    void testSaveRejectsBlankUsername() {
        Utente u = new Utente("   ", "HASH");
        assertThrows(IllegalArgumentException.class, () -> repo.save(u));
    }

    @Test
    void testSaveRejectsBlankPasswordHash() {
        Utente u = new Utente("mario", "   ");
        assertThrows(IllegalArgumentException.class, () -> repo.save(u));
    }

    @Test
    void testFindByUsernameNullReturnsNull() {
        assertNull(repo.findByUsername(null));
    }

    // ============================================================
    // UPDATE 
    // ============================================================
    @Test
    void testUpdateExistingUser() {
        repo.save(new Utente("pippo", "OLD"));

        // stesso username ma case diverso -> deve sovrascrivere
        repo.save(new Utente("PIPPO", "NEW"));

        Utente found = repo.findByUsername("pippo");
        assertNotNull(found);
        assertEquals("new", found.getPasswordHash().toLowerCase());
        
    }

    // ============================================================
    // SNAPSHOT IMMUTABILE
    // ============================================================
    @Test
    void testFindAllReturnsImmutableSnapshot() {
        repo.save(new Utente("a", "H1"));

        Collection<Utente> all = repo.findAll();
        assertThrows(UnsupportedOperationException.class, all::clear);
    }
}
