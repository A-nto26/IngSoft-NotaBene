package com.sweng.notes.service;

import com.sweng.notes.dto.CreateNoteRequest;
import com.sweng.notes.dto.NoteUpdateRequest;
import com.sweng.notes.dto.ShareNoteRequest;
import com.sweng.notes.model.*;

import com.sweng.notes.repository.NoteRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NoteServiceTest {

    private NoteRepository repo;
    private NoteService service;

    @BeforeEach
    void setup() {
        repo = mock(NoteRepository.class);
        service = new NoteService(repo);
    }

    // ============================================================
    // CREATE
    // ============================================================

    @Test
    void testCreate_ok_normalizzaCreatore_ePermesso() {
        CreateNoteRequest req = new CreateNoteRequest();
        req.setTitolo("Titolo");
        req.setContenuto("Contenuto");
        req.setCreatore("  MARIO  ");
        req.setPermesso("SCRITTURA");

        Note created = service.create(req);

        assertNotNull(created);
        assertEquals("mario", created.getCreatore());
        assertTrue(created.getPermesso() instanceof Scrittura);

        verify(repo).save(any(Note.class));
    }

    @Test
    void testCreate_fail_reqNull() {
        assertThrows(ResponseStatusException.class, () -> service.create(null));
        verifyNoInteractions(repo);
    }

    // ============================================================
    // GET VISIBLE
    // ============================================================

    @Test
    void testGetVisibleNotesForUser_ok() {
        when(repo.findByCreator("mario"))
                .thenReturn(List.of(new Note(1, "A", "B", "mario", null)));
        when(repo.findSharedWithUser("mario"))
                .thenReturn(List.of(new Note(2, "C", "D", "anna", null)));

        List<Note> res = service.getVisibleNotesForUser("mario");

        assertNotNull(res);
        assertEquals(2, res.size());

        verify(repo).findByCreator("mario");
        verify(repo).findSharedWithUser("mario");
    }

    // ============================================================
    // DELETE
    // ============================================================

    @Test
    void testDelete_ok() {
        Note n = new Note(10, "T", "C", "mario", null);
        when(repo.findById(10)).thenReturn(n);

        service.delete(10, "mario");

        verify(repo).delete(10);
    }

    // ============================================================
    // DUPLICATE
    // ============================================================

    @Test
    void testDuplicate_ok_creaCopiaPrivata() {
        Note orig = new Note(1, "Titolo", "Contenuto", "mario", "casa");
        orig.setPermesso(new Lettura());                 // non fondamentale se sei autore, ma ok
        orig.setUtentiCondivisi(new LinkedHashSet<>());  // sicurezza

        when(repo.findById(1)).thenReturn(orig);

        Note copia = service.duplicate(1, "mario");

        assertNotNull(copia);
        assertTrue(copia.getTitolo().contains("(Copia)"));
        assertEquals("mario", copia.getCreatore());
        assertTrue(copia.getPermesso() instanceof Privata);

        verify(repo).save(any(Note.class));
    }

    // ============================================================
    // REMOVE SELF
    // ============================================================

    @Test
    void testRemoveSelf_ok() {
        Note n = new Note(5, "T", "C", "mario", null);
        n.setUtentiCondivisi(new LinkedHashSet<>(List.of("anna")));

        when(repo.findById(5)).thenReturn(n);

        service.removeSelf(5, "anna");

        verify(repo).removeSelf(5, "anna");
    }

    // ============================================================
    // SHARE
    // ============================================================

    @Test
    void testShareNote_ok_aggiungeUtenti() {
        Note n = new Note(7, "T", "C", "mario", null);
        when(repo.findById(7)).thenReturn(n);

        ShareNoteRequest req = new ShareNoteRequest();
        req.setUtentiCondivisi(List.of("anna", "luca"));

        service.shareNote(7, req, "mario");

        verify(repo).addUsersToShare(eq(7), anySet());
    }

    // ============================================================
    // UPDATE (semplice)
    // ============================================================

    @Test
    void testUpdate_ok_conLock() {
        Note n = new Note(3, "Old", "C", "mario", null);
        n.setPermesso(new Scrittura());          // evita NPE su getPermesso()
        n.setVersioni(new ArrayList<>());        // evita problemi su salvaVersionePrecedente
        n.setCreatedAt(LocalDateTime.now());
        n.setLastModifiedAt(LocalDateTime.now());
        n.setLastModifiedBy("mario");

        when(repo.findById(3)).thenReturn(n);
        when(repo.getEffectiveLockOwner(3)).thenReturn(Optional.of("mario"));

        NoteUpdateRequest req = new NoteUpdateRequest();
        req.setTitolo("New");
        req.setContenuto("NewC");
        // niente req.setPermesso(...) per non cambiare permesso in questo test

        Note updated = service.update(3, req, "mario");

        assertNotNull(updated);
        assertEquals("New", updated.getTitolo());

        verify(repo).save(n);
    }

    // ============================================================
    // LOCK (semplice)
    // ============================================================

    @Test
    void testLock_notFound() {
        when(repo.findById(99)).thenReturn(null);

        String res = service.lock(99, "mario");

        assertEquals("not_found", res);
    }

    @Test
    void testLock_expiredRecovered() {
        Note n = new Note(1, "T", "C", "mario", null);

        when(repo.findById(1)).thenReturn(n);
        when(repo.getEffectiveLockOwner(1)).thenReturn(Optional.empty());
        when(repo.tryLock(1, "mario")).thenReturn(true);

        String res = service.lock(1, "mario");

        assertEquals("expired_recovered", res);
        verify(repo).tryLock(1, "mario");
    }

    @Test
    void testLock_alreadyLocked() {
        Note n = new Note(2, "T", "C", "mario", null);

        when(repo.findById(2)).thenReturn(n);
        when(repo.getEffectiveLockOwner(2)).thenReturn(Optional.of("anna"));

        String res = service.lock(2, "mario");

        assertEquals("already_locked", res);
    }
}
