package com.sweng.notes.repository;

import com.sweng.notes.model.Cartella;
import com.sweng.notes.model.Note;

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
        // IMPORTANTE: il repo chiama sempre db.commit()
        // quindi abilitiamo le transazioni anche in memoryDB
        db = DBMaker.memoryDB()
                .transactionEnable()
                .make();

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
    // SAVE + FIND
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
    void testFindAllReturnsNotes() {
        repo.save(new Note(0, "A", "C", "u", ""));
        repo.save(new Note(0, "B", "C", "u", ""));

        List<Note> all = repo.findAll();
        assertEquals(2, all.size());

    
    }

    // ============================================================
    // FOLDERS 
    // ============================================================
    @Test
    void testDeleteDoesNotRemoveFolder() {
        Note n = new Note(0, "T", "C", "mario", "lavoro");
        repo.save(n);

        assertNotNull(repo.findFolderByName("lavoro"));

        repo.delete(n.getId());

        assertNull(repo.findById(n.getId()));

        assertNotNull(repo.findFolderByName("lavoro"));
    }

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
    void testDeleteFolderDetachesNotes() {
        Note n = new Note(0, "T", "C", "user", "personale");
        repo.save(n);

        assertNotNull(repo.findFolderByName("personale"));

        repo.deleteFolder("personale");

        // cartella rimossa
        assertNull(repo.findFolderByName("personale"));

      
        Note updated = repo.findById(n.getId());
        assertNotNull(updated);
        assertNull(updated.getCartella());
    }

    // ============================================================
    // SHARING
    // ============================================================
    @Test
    void testAddUsersToShare() {
        Note n = new Note(0, "T", "C", "anna", "");
        repo.save(n);

        repo.addUsersToShare(n.getId(), Set.of("luca", "paolo"));

        Note updated = repo.findById(n.getId());
        assertNotNull(updated);
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
        assertNotNull(updated);
        assertFalse(updated.getUtentiCondivisi().contains("luca"));
    }

    @Test
    void testFindSharedWithUser() {
        Note n1 = new Note(0, "A", "C", "anna", "");
        n1.setUtentiCondivisi(new LinkedHashSet<>(List.of("luca")));
        repo.save(n1);

        Note n2 = new Note(0, "B", "C", "anna", "");
        n2.setUtentiCondivisi(new LinkedHashSet<>(List.of("luca")));
        repo.save(n2);

        List<Note> shared = repo.findSharedWithUser("luca");
        assertEquals(2, shared.size());
    }

    // ============================================================
    // LOCK SYSTEM 
    // ============================================================
    @Test
    void testLockUnlock_Owner() {
        Note n = new Note(0, "T", "C", "anna", "");
        repo.save(n);

        assertTrue(repo.lockNote(n.getId(), "luca"));

        Map<String, Object> state = repo.getLockState(n.getId());
        assertEquals(true, state.get("locked"));
        assertEquals("luca", state.get("lockedBy"));
        assertNotNull(state.get("lockedAt")); 

        
        assertTrue(repo.unlockNote(n.getId(), "luca"));

        Map<String, Object> after = repo.getLockState(n.getId());
        assertEquals(false, after.get("locked"));
    }

    @Test
    void testLockCannotOverrideDifferentUser() {
        Note n = new Note(0, "T", "C", "anna", "");
        repo.save(n);

        assertTrue(repo.lockNote(n.getId(), "luca"));
        assertFalse(repo.lockNote(n.getId(), "marco"));

        Map<String, Object> state = repo.getLockState(n.getId());
        assertEquals(true, state.get("locked"));
        assertEquals("luca", state.get("lockedBy"));
    }

    @Test
    void testUnlockDifferentUserFails() {
        Note n = new Note(0, "T", "C", "anna", "");
        repo.save(n);

        assertTrue(repo.lockNote(n.getId(), "luca"));

        // unlock da utente diverso (lock non scaduto) -> false
        assertFalse(repo.unlockNote(n.getId(), "marco"));

        Map<String, Object> state = repo.getLockState(n.getId());
        assertEquals(true, state.get("locked"));
        assertEquals("luca", state.get("lockedBy"));
    }
}
