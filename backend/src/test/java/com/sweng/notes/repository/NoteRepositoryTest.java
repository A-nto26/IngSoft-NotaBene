package com.sweng.notes.repository;

import com.sweng.notes.model.Note;
import com.sweng.notes.model.Cartella;
import com.sweng.notes.model.VersioneNota;
import org.junit.jupiter.api.Test;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.*;

class NoteRepositoryTest {

    // ============================================================
    // FACTORY: crea repository MapDB in-memory
    // ============================================================
    private NoteRepository createRepo() {
        DB db = DBMaker.memoryDB().make();
        ConcurrentMap<Integer, Note> notes = new ConcurrentHashMap<>();
        ConcurrentMap<String, Cartella> folders = new ConcurrentHashMap<>();
        return new NoteRepository(db, notes, folders);
    }

    // ============================================================
    // SALVATAGGIO E LETTURA
    // ============================================================
    @Test
    void testSaveAndFindById() {
        NoteRepository repo = createRepo();

        Note n = new Note(0, "Titolo", "Contenuto", "user1", "casa");
        repo.save(n);

        Note found = repo.findById(n.getId());

        assertNotNull(found);
        assertEquals("Titolo", found.getTitolo());
        assertEquals("casa", found.getCartella());
    }

    @Test
    void testFindAllSorted() {
        NoteRepository repo = createRepo();

        Note a = new Note(0, "A", "C", "u1", "");
        Note b = new Note(0, "B", "C", "u1", "");

        repo.save(a);
        repo.save(b);

        List<Note> all = repo.findAll();
        assertEquals(2, all.size());
    }

    // ============================================================
    // DELETE + CARTELLE IN AUTOMATICO
    // ============================================================
    @Test
    void testDeleteRemovesNoteAndFolderIfEmpty() {
        NoteRepository repo = createRepo();

        Note n = new Note(0, "T", "C", "mario", "lavoro");
        repo.save(n);

        assertNotNull(repo.findFolderByName("lavoro"));

        repo.delete(n.getId());

        assertNull(repo.findById(n.getId()));
        assertNull(repo.findFolderByName("lavoro"));
    }

    // ============================================================
    // CARTELLE
    // ============================================================
    @Test
    void testCreateAndFindFolder() {
        NoteRepository repo = createRepo();

        repo.createFolder("Casa", "mario", "#FFAA00");

        Cartella c = repo.findFolderByName("casa");
        assertNotNull(c);
        assertEquals("Casa", c.getNome());
        assertEquals("mario", c.getCreatore());
        assertEquals("#FFAA00", c.getColore());
    }

    @Test
    void testDeleteFolderDoesNotRemoveNotes() {
        NoteRepository repo = createRepo();

        Note n = new Note(0, "T", "C", "user", "personale");
        repo.save(n);

        assertEquals(1, repo.findByCartella("personale").size());

        repo.deleteFolder("personale");

        assertNull(repo.findFolderByName("personale"));

        Note updated = repo.findById(n.getId());
        assertNull(updated.getCartella());
    }

    @Test
    void testFindByCartella() {
        NoteRepository repo = createRepo();

        Note a = new Note(0, "A", "C", "u", "casa");
        Note b = new Note(0, "B", "C", "u", "casa");

        repo.save(a);
        repo.save(b);

        List<Note> result = repo.findByCartella("casa");
        assertEquals(2, result.size());
    }

    // ============================================================
    // VERSIONAMENTO (aggiornato per nuova firma e logica)
    // ============================================================
    @Test
    void testRestoreVersion() {
        NoteRepository repo = createRepo();

        // Nota iniziale
        Note n = new Note(0, "Titolo1", "Contenuto1", "mario", "");
        repo.save(n);

        // Simuliamo un update: salva versione precedente + cambia titolo
        n.salvaVersionePrecedente(); // salva Titolo1 / Contenuto1
        n.setTitolo("Titolo2");
        n.setContenuto("Cont2");
        repo.save(n);

        // Restore della versione 0 → ritorna a Titolo1/Contenuto1
        repo.restoreVersion(n.getId(), 0, "mario");

        Note restored = repo.findById(n.getId());

        // 🔍 1) Deve avere di nuovo i valori originali
        assertEquals("Titolo1", restored.getTitolo());
        assertEquals("Contenuto1", restored.getContenuto());

        // 🔍 2) Le versioni devono essere:
        //     [ versione_pre_restore ] → cioè Titolo2/Cont2
        assertEquals(1, restored.getVersioni().size());
        VersioneNota v = restored.getVersioni().get(0);
        assertEquals("Titolo2", v.getTitolo());
        assertEquals("Cont2", v.getContenuto());

        // 🔍 3) lastModifiedBy aggiornato
        assertEquals("mario", restored.getLastModifiedBy());
    }

    // ============================================================
    // CONDIVISIONE
    // ============================================================
    @Test
    void testAddUsersToShare() {
        NoteRepository repo = createRepo();

        Note n = new Note(0, "T", "C", "anna", "");
        repo.save(n);

        repo.addUsersToShare(n.getId(), Set.of("luca", "paolo"));

        Note updated = repo.findById(n.getId());
        assertTrue(updated.getUtentiCondivisi().contains("luca"));
        assertTrue(updated.getUtentiCondivisi().contains("paolo"));
    }

    @Test
    void testRemoveSelf() {
        NoteRepository repo = createRepo();

        Note n = new Note(0, "T", "C", "anna", "");
        n.setUtentiCondivisi(new LinkedHashSet<>(List.of("anna", "luca", "paolo")));
        repo.save(n);

        repo.removeSelf(n.getId(), "luca");

        Note updated = repo.findById(n.getId());
        assertFalse(updated.getUtentiCondivisi().contains("luca"));
    }

    @Test
    void testFindSharedWithUser() {
        NoteRepository repo = createRepo();

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
    // LOCK SYSTEM
    // ============================================================
    @Test
    void testLockUnlock() {
        NoteRepository repo = createRepo();

        Note n = new Note(0, "T", "C", "anna", "");
        repo.save(n);

        assertTrue(repo.lockNote(n.getId(), "luca"));

        Map<String, Object> state = repo.getLockState(n.getId());
        assertEquals("luca", state.get("lockedBy"));
        assertNotNull(state.get("lockedAt"));

        assertTrue(repo.unlockNote(n.getId(), "luca"));

        Map<String, Object> after = repo.getLockState(n.getId());
        assertNull(after.get("lockedBy"));
        assertNull(after.get("lockedAt"));
    }

    @Test
    void testForceUnlock() {
        NoteRepository repo = createRepo();

        Note n = new Note(0, "T", "C", "anna", "");
        repo.save(n);

        repo.lockNote(n.getId(), "luca");
        repo.forceUnlock(n.getId());

        Map<String, Object> state = repo.getLockState(n.getId());
        assertNull(state.get("lockedBy"));
        assertNull(state.get("lockedAt"));
    }

    @Test
    void testLockCannotOverrideDifferentUser() {
        NoteRepository repo = createRepo();

        Note n = new Note(0, "T", "C", "anna", "");
        repo.save(n);

        assertTrue(repo.lockNote(n.getId(), "luca"));
        assertFalse(repo.lockNote(n.getId(), "marco"));

        Map<String, Object> state = repo.getLockState(n.getId());
        assertEquals("luca", state.get("lockedBy"));
    }
}
