package com.sweng.notes.service;

import com.sweng.notes.dto.CreateNoteRequest;
import com.sweng.notes.dto.NoteUpdateRequest;
import com.sweng.notes.dto.ShareNoteRequest;

import com.sweng.notes.model.*;
import com.sweng.notes.repository.NoteRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class NoteServiceTest {

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
    void testCreateNoteSuccess() {

        CreateNoteRequest req = new CreateNoteRequest();
        req.setTitolo("Titolo");
        req.setContenuto("Contenuto");
        req.setCreatore("mario");
        req.setCartella("casa");
        req.setColoreCartella("#ff8844");
        req.setPermesso("SCRITTURA");
        req.setUtentiCondivisi(List.of("anna", "luca"));

        // Simuliamo assegnazione ID
        doAnswer(invocation -> {
            Note n = invocation.getArgument(0);
            n.setId(10);
            return null;
        }).when(repo).save(any(Note.class));

        Note result = service.create(req);

        assertNotNull(result);
        assertEquals("Titolo", result.getTitolo());
        assertEquals("mario", result.getCreatore());
        assertEquals("casa", result.getCartella());
        assertEquals("#ff8844", result.getColoreCartella());
        assertTrue(result.getPermesso() instanceof Scrittura);
        assertTrue(result.getUtentiCondivisi().contains("anna"));
        assertTrue(result.getUtentiCondivisi().contains("luca"));

        verify(repo).save(any(Note.class));
    }

    // ============================================================
    // UPDATE - versioning Sprint 4
    // ============================================================
    @Test
    void testUpdateCreatesVersion() {

        Note existing = new Note(5, "T1", "C1", "mario", "casa");
        existing.setVersioni(new ArrayList<>());
        existing.setLockedBy("mario"); // lock valido
        existing.setLockedAt(LocalDateTime.now());

        when(repo.findById(5)).thenReturn(existing);
        when(repo.getEffectiveLockOwner(5)).thenReturn(Optional.of("mario"));

        NoteUpdateRequest req = new NoteUpdateRequest();
        req.setTitolo("T2");
        req.setContenuto("C2");

        service.update(5, req, "mario");

        assertEquals(1, existing.getVersioni().size());
        VersioneNota saved = existing.getVersioni().get(0);
        assertEquals("T1", saved.getTitolo());

        verify(repo).save(existing);
    }

    @Test
    void testUpdateForbiddenIfNotWriter() {

        Note existing = new Note(5, "T1", "C1", "mario", "casa");
        existing.setPermesso(new Privata());
        when(repo.findById(5)).thenReturn(existing);

        assertThrows(RuntimeException.class,
                () -> service.update(5, new NoteUpdateRequest(), "anna"));
    }

    // ============================================================
    // DELETE
    // ============================================================
    @Test
    void testDeleteSuccess() {
        Note n = new Note(20, "T", "C", "mario", null);
        when(repo.findById(20)).thenReturn(n);

        service.delete(20, "mario");
        verify(repo).delete(20);
    }

    @Test
    void testDeleteForbidden() {
        Note n = new Note(20, "T", "C", "mario", null);
        when(repo.findById(20)).thenReturn(n);

        assertThrows(RuntimeException.class,
                () -> service.delete(20, "luca"));
    }

    // ============================================================
    // DUPLICATE
    // ============================================================
    @Test
    void testDuplicateCreatesPrivateCopy() {

        Note orig = new Note(1, "Titolo", "Contenuto", "mario", "");
        orig.setPermesso(new Lettura());
        orig.getUtentiCondivisi().add("anna");

        when(repo.findById(1)).thenReturn(orig);

        doAnswer(invocation -> {
            Note saved = invocation.getArgument(0);
            saved.setId(2);
            return null;
        }).when(repo).save(any(Note.class));

        Note copia = service.duplicate(1, "anna");

        assertEquals("Titolo (Copia)", copia.getTitolo());
        assertEquals("anna", copia.getCreatore());
        assertTrue(copia.getUtentiCondivisi().isEmpty());
        assertTrue(copia.getPermesso() instanceof Privata);

        verify(repo).save(any(Note.class));
    }

    // ============================================================
    // SEARCH
    // ============================================================
    @Test
    void testSearchFiltersByQuery() {
        Note own = new Note(1, "Spesa", "latte", "mario", "");
        Note shared = new Note(2, "Lavoro", "meeting", "anna", "");
        shared.getUtentiCondivisi().add("mario");

        when(repo.findByCreator("mario")).thenReturn(List.of(own));
        when(repo.findSharedWithUser("mario")).thenReturn(List.of(shared));

        List<Note> result = service.search("mario", "latte");

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getId());
    }

    // ============================================================
    // SET CARTELLA
    // ============================================================
    @Test
    void testSetCartellaSuccess() {
        Note n = new Note(10, "T", "C", "mario", "old");
        when(repo.findById(10)).thenReturn(n);

        service.setCartella(10, "casa", "mario");

        assertEquals("casa", n.getCartella());
        verify(repo).save(n);
    }

    @Test
    void testSetCartellaForbidden() {
        Note n = new Note(10, "T", "C", "mario", "old");
        when(repo.findById(10)).thenReturn(n);

        assertThrows(RuntimeException.class,
                () -> service.setCartella(10, "casa", "anna"));
    }

    // ============================================================
    // SHARE
    // ============================================================
    @Test
    void testShareAddsUsers() {
        Note n = new Note(5, "T", "C", "mario", "");
        when(repo.findById(5)).thenReturn(n);

        ShareNoteRequest req = new ShareNoteRequest();
        req.setUtentiCondivisi(List.of("luca", "anna"));

        service.shareNote(5, req, "mario");

        verify(repo).addUsersToShare(eq(5), anySet());
    }

    @Test
    void testShareForbidden() {
        Note n = new Note(5, "T", "C", "mario", "");
        when(repo.findById(5)).thenReturn(n);

        ShareNoteRequest req = new ShareNoteRequest();
        req.setUtentiCondivisi(List.of("luca"));

        assertThrows(RuntimeException.class,
                () -> service.shareNote(5, req, "anna"));
    }

    // ============================================================
    // REMOVE SELF
    // ============================================================
    @Test
    void testRemoveSelfSuccess() {
        Note n = new Note(5, "T", "C", "mario", "");
        n.getUtentiCondivisi().add("anna");
        when(repo.findById(5)).thenReturn(n);

        service.removeSelf(5, "anna");

        verify(repo).removeSelf(5, "anna");
    }

    @Test
    void testRemoveSelfForbidden() {
        Note n = new Note(5, "T", "C", "mario", "");
        when(repo.findById(5)).thenReturn(n);

        assertThrows(RuntimeException.class,
                () -> service.removeSelf(5, "anna"));
    }

    // ============================================================
    // RESTORE VERSION — Sprint 4
    // ============================================================
    @Test
    void testRestoreVersionAllowedForAutore() {
        Note n = new Note(7, "T", "C", "mario", "");
        n.setVersioni(List.of(
                new VersioneNota("oldT", "oldC", LocalDateTime.now())
        ));
        when(repo.findById(7)).thenReturn(n);
        when(repo.getEffectiveLockOwner(7)).thenReturn(Optional.empty());

        service.restoreVersion(7, 0, "mario");

        verify(repo).save(n);
    }

    @Test
    void testRestoreVersionForbiddenForLettura() {
        Note n = new Note(7, "T", "C", "mario", "");
        n.setPermesso(new Lettura());
        n.getUtentiCondivisi().add("anna");
        n.setVersioni(List.of(new VersioneNota("old", "old", LocalDateTime.now())));

        when(repo.findById(7)).thenReturn(n);

        assertThrows(RuntimeException.class,
                () -> service.restoreVersion(7, 0, "anna"));
    }
}