package com.sweng.notes.repository;

import com.sweng.notes.model.Utente;
import org.junit.jupiter.api.*;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryTest {

    private DB db;
    private Map<String, Utente> map;
    private UserRepository repo;

    @BeforeEach
    void setup() {
        db = DBMaker.memoryDB().make();
        map = new HashMap<>();
        repo = new UserRepository(db, map);
    }

    @AfterEach
    void cleanup() {
        db.close();
    }

    // ============================================================
    // 🔵 SPRINT 3 – TEST BASE (INVARIATI)
    // ============================================================

    @Test
    void testSaveAndFindUser() {
        Utente u = new Utente("Mario", "1234");
        repo.save(u);

        Utente found = repo.findByUsername("mario");
        assertNotNull(found);
        assertEquals("mario", found.getUsername());
    }

    @Test
    void testExists() {
        repo.save(new Utente("Anna", "pass"));

        assertTrue(repo.exists("anna"));
        assertTrue(repo.exists("ANNA"));
        assertFalse(repo.exists("xxx"));
    }

    @Test
    void testFindAllUsers() {
        repo.save(new Utente("a", "1"));
        repo.save(new Utente("b", "2"));

        assertEquals(2, repo.findAll().size());
    }

    // ============================================================
    // 🔥 SPRINT 4 – NUOVI TEST AVANZATI
    // ============================================================

    @Test
    void testSaveNormalizesUsernameInsideModel() {
        Utente u = new Utente(" TeStUser ", "pw");
        repo.save(u);

        Utente found = repo.findByUsername("testuser");
        assertNotNull(found);
        assertEquals("testuser", found.getUsername());
    }

    @Test
    void testSaveDoesNotAcceptNullUser() {
        assertThrows(IllegalArgumentException.class, () -> repo.save(null));
    }

    @Test
    void testSaveDoesNotAcceptNullUsernameInsideModel() {
        Utente u = new Utente(null, "pw");

        // Deve lanciare eccezione
        assertThrows(IllegalArgumentException.class, () -> repo.save(u));

        // Non deve salvare nulla
        assertEquals(0, repo.findAll().size());
    }

    @Test
    void testUpdateExistingUser() {
        repo.save(new Utente("pippo", "oldpass"));

        Utente updated = new Utente("PIPPO", "newpass");
        repo.save(updated);

        Utente found = repo.findByUsername("pippo");

        assertNotNull(found);
        assertEquals("newpass", found.getPasswordHash());
    }

    @Test
    void testFindByUsernameNullReturnsNull() {
        assertNull(repo.findByUsername(null));
    }

    @Test
    void testExistsNullReturnsFalse() {
        assertFalse(repo.exists(null));
    }

    @Test
    void testFindAllReturnsImmutableSnapshot() {
        repo.save(new Utente("a", "1"));

        Collection<Utente> users = repo.findAll();
        assertThrows(UnsupportedOperationException.class, () -> users.clear());
    }

    @Test
    void testMultipleUsersGetNormalizedKeys() {
        repo.save(new Utente("Mario", "1"));
        repo.save(new Utente("mArIo", "2")); // aggiornamento

        Utente found = repo.findByUsername("MARIO");

        assertNotNull(found);
        assertEquals("2", found.getPasswordHash());
    }

    @Test
    void testRepositoryInMemoryDoesNotAffectFileDB() {
        Map<String, Utente> externalMap = new HashMap<>();
        UserRepository r2 = new UserRepository(DBMaker.memoryDB().make(), externalMap);

        r2.save(new Utente("x", "pw"));

        assertEquals(1, r2.findAll().size());
        assertEquals(0, repo.findAll().size());
    }

    @Test
    void testCommitIsCalledOnSave() {
        Utente u = new Utente("tester", "pw");

        assertDoesNotThrow(() -> repo.save(u));
        assertNotNull(repo.findByUsername("tester"));
    }
}