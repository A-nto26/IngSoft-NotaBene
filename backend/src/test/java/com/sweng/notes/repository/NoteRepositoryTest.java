package com.sweng.notes.repository;

import com.sweng.notes.model.Note;
import com.sweng.notes.model.Cartella;

import org.junit.jupiter.api.*;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class NoteRepositoryTest {

    private NoteRepository repo;
    private DB db;

    @BeforeEach
    void setup() {
        db = DBMaker.memoryDB().make();

        repo = new NoteRepository(
                db,
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>()
        );
    }

    @AfterEach
    void cleanup() {
        repo.close();
    }

    // ============================================================
    // SALVATAGGIO + LETTURA
    // ============================================================
    @Test
    void testSaveAndFindById() {
        Note n = new Note(0, "Titolo", "Contenuto", "user1", "casa");
        repo.save(n);

        Note found = repo.findById(n.getId());
        assertNotNull(found);
        assertEquals("Titolo", found.getTitolo());
        assertEquals("casa", found.getCartella());
    }

    @Test
    void testFindAllSorted() {
        Note a = new Note(0, "A", "C", "u", "");
        Note b = new Note(0, "B", "C", "u", "");

        repo.save(a);
        repo.save(b);

        List<Note> all = repo.findAll();
        assertEquals(2, all.size());
    }

    // ============================================================
    // DELETE + CARTELLE – Sprint 4
    // ============================================================
    @Test
    void testDeleteDoesNotRemoveFolder_Sprint4() {
        Note n = new Note(0, "T", "C", "mario", "lavoro");
        repo.save(n);

        assertNotNull(repo.findFolderByName("lavoro"));

        repo.delete(n.getId());

        assertNull(repo.findById(n.getId()));
        // La cartella deve rimanere
        assertNotNull(repo.findFolderByName("lavoro"));
    }

    // ============================================================
    // CARTELLE
    // ============================================================
    @Test
    void testCreateAndFindFolder() {
        repo.createFolder("Casa", "mario", "#FFAA00");

        Cartella c = repo.findFolderByName("casa");
        assertNotNull(c);
        assertEquals("Casa", c.getNome());
        assertEquals("mario", c.getCreatore());
        assertEquals("#FFAA00", c.getColore());
    }

    @Test
    void testDeleteFolder_Sprint4() {
        Note n = new Note(0, "T", "C", "user", "personale");
        repo.save(n);

        repo.deleteFolder("personale");

        assertNull(repo.findFolderByName("personale"));

        Note updated = repo.findById(n.getId());
        assertNull(updated.getCartella());
    }

    // ============================================================
    // CONDIVISIONE
    // ============================================================
    @Test
    void testAddUsersToShare() {
        Note n = new Note(0, "T", "C", "anna", "");
        repo.save(n);

        repo.addUsersToShare(n.getId(), Set.of("luca", "paolo"));

        Note updated = repo.findById(n.getId());
        assertTrue(updated.getUtentiCondivisi().contains("luca"));
        assertTrue(updated.getUtentiCondivisi().contains("paolo"));
    }

    @Test
    void testRemoveSelf() {
        Note n = new Note(0, "T", "C", "anna", "");
        n.setUtentiCondivisi(new LinkedHashSet<>(List.of("anna", "luca", "paolo")));
        repo.save(n);

        repo.removeSelf(n.getId(), "luca");

        Note updated = repo.findById(n.getId());
        assertFalse(updated.getUtentiCondivisi().contains("luca"));
    }

    @Test
    void testFindSharedWithUser() {
        Note n1 = new Note(0, "A", "C", "anna", "");
        n1.setUtentiCondivisi(Set.of("luca"));
        repo.save(n1);

        Note n2 = new Note(0, "B", "C", "anna", "");
        n2.setUtentiCondivisi(Set.of("luca"));
        repo.save(n2);

        List<Note> shared = repo.findSharedWithUser("luca");
        assertEquals(2, shared.size());
    }

    // ============================================================
    // LOCK SYSTEM — Sprint 4
    // ============================================================
    @Test
    void testLockUnlock() {
        Note n = new Note(0, "T", "C", "anna", "");
        repo.save(n);

        assertTrue(repo.lockNote(n.getId(), "luca"));

        Map<String, Object> state = repo.getLockState(n.getId());
        assertEquals("luca", state.get("lockedBy"));
        assertNotNull(state.get("lockedAt"));

        assertTrue(repo.unlockNote(n.getId(), "luca"));

        Map<String, Object> after = repo.getLockState(n.getId());
        assertEquals(false, after.get("locked"));
    }

    @Test
    void testForceUnlock() {
        Note n = new Note(0, "T", "C", "anna", "");
        repo.save(n);

        repo.lockNote(n.getId(), "luca");
        repo.forceUnlock(n.getId());

        Map<String, Object> state = repo.getLockState(n.getId());
        assertEquals(false, state.get("locked"));
    }

    @Test
    void testLockCannotOverrideDifferentUser() {
        Note n = new Note(0, "T", "C", "anna", "");
        repo.save(n);

        assertTrue(repo.lockNote(n.getId(), "luca"));
        assertFalse(repo.lockNote(n.getId(), "marco"));

        assertEquals("luca", repo.getLockState(n.getId()).get("lockedBy"));
    }
}