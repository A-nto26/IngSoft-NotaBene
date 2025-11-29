package com.sweng.notes.service;

import com.sweng.notes.dto.CreateNoteRequest;
import com.sweng.notes.dto.NoteUpdateRequest;
import com.sweng.notes.dto.ShareNoteRequest;

import com.sweng.notes.model.Note;
import com.sweng.notes.model.VersioneNota;
import com.sweng.notes.model.Scrittura;
import com.sweng.notes.model.Lettura;

import com.sweng.notes.repository.NoteRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        req.setPermesso("SCRITTURA");
        req.setUtentiCondivisi(List.of("anna"));

        doAnswer(invocation -> {
            Note saved = invocation.getArgument(0);
            saved.setId(10);
            return null;
        }).when(repo).save(any(Note.class));

        Note result = service.create(req);

        assertNotNull(result);
        assertEquals("Titolo", result.getTitolo());
        assertEquals("mario", result.getCreatore());
        assertEquals("casa", result.getCartella());
        assertTrue(result.getUtentiCondivisi().contains("anna"));

        verify(repo).save(any());
    }

    // ============================================================
    // UPDATE
    // ============================================================
    @Test
    void testUpdateCreatesVersion() {
        Note existing = new Note(5, "T1", "C1", "mario", "casa");
        when(repo.findById(5)).thenReturn(existing);

        NoteUpdateRequest req = new NoteUpdateRequest();
        req.setTitolo("T2");
        req.setContenuto("C2");

        service.update(5, req, "mario");

        assertEquals(1, existing.getVersioni().size());
        VersioneNota v = existing.getVersioni().get(0);

        assertEquals("T1", v.getTitolo());
        assertEquals("C1", v.getContenuto());

        verify(repo).save(existing);
    }

    @Test
    void testUpdateForbiddenIfNotAutore() {
        Note existing = new Note(5, "T1", "C1", "mario", "casa");
        when(repo.findById(5)).thenReturn(existing);

        NoteUpdateRequest req = new NoteUpdateRequest();
        req.setTitolo("T2");
        req.setContenuto("C2");

        assertThrows(RuntimeException.class,
                () -> service.update(5, req, "anna"));
    }

    // ============================================================
    // DELETE
    // ============================================================
    @Test
    void testDeleteSuccess() {
        Note existing = new Note(20, "T", "C", "mario", null);
        when(repo.findById(20)).thenReturn(existing);

        service.delete(20, "mario");
        verify(repo).delete(20);
    }

    @Test
    void testDeleteForbiddenIfNotAutore() {
        Note existing = new Note(20, "T", "C", "mario", null);
        when(repo.findById(20)).thenReturn(existing);

        assertThrows(RuntimeException.class,
                () -> service.delete(20, "luca"));
    }

    // ============================================================
    // DUPLICATE
    // ============================================================
    @Test
    void testDuplicateCreatesPrivateCopy() {
        Note n = new Note(1, "Titolo", "Contenuto", "mario", "");
        n.setPermesso(new Lettura());
        n.getUtentiCondivisi().add("anna");

        when(repo.findById(1)).thenReturn(n);

        doAnswer(invocation -> {
            Note saved = invocation.getArgument(0);
            saved.setId(2);
            return null;
        }).when(repo).save(any(Note.class));

        Note copia = service.duplicate(1, "anna");

        assertNotNull(copia);
        assertEquals("Titolo (Copia)", copia.getTitolo());
        assertEquals("Contenuto", copia.getContenuto());
        assertEquals("anna", copia.getCreatore());
        assertTrue(copia.getUtentiCondivisi().isEmpty());
    }

    // ============================================================
    // SEARCH
    // ============================================================
    @Test
    void testSearchVisibleNotes() {
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
        Note n = new Note(10, "T", "C", "mario", "vecchia");
        when(repo.findById(10)).thenReturn(n);

        service.setCartella(10, "casa", "mario");

        assertEquals("casa", n.getCartella());
        verify(repo).save(n);
    }

    @Test
    void testSetCartellaForbidden() {
        Note n = new Note(10, "T", "C", "mario", "vecchia");
        when(repo.findById(10)).thenReturn(n);

        assertThrows(RuntimeException.class,
                () -> service.setCartella(10, "casa", "luca"));
    }

    // ============================================================
    // SHARE
    // ============================================================
    @Test
    void testShareNoteAddsUsers() {
        Note n = new Note(5, "T", "C", "mario", "");
        when(repo.findById(5)).thenReturn(n);

        ShareNoteRequest req = new ShareNoteRequest();
        req.setUtentiCondivisi(List.of("luca", "anna"));

        service.shareNote(5, req, "mario");

        verify(repo).addUsersToShare(eq(5), anySet());
    }

    @Test
    void testShareNoteForbiddenIfNotAutore() {
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
    void testRemoveSelfForbiddenIfNotInList() {
        Note n = new Note(5, "T", "C", "mario", "");
        when(repo.findById(5)).thenReturn(n);

        assertThrows(RuntimeException.class,
                () -> service.removeSelf(5, "anna"));
    }

    // ============================================================
    // RESTORE VERSION (✔ aggiornato)
    // ============================================================
    @Test
    void testRestoreVersionAllowedForAutore() {
        Note n = new Note(7, "T", "C", "mario", "");
        when(repo.findById(7)).thenReturn(n);

        service.restoreVersion(7, 0, "mario");

        verify(repo).restoreVersion(7, 0, "mario");
    }

    @Test
    void testRestoreVersionAllowedForScrittura() {
        Note n = new Note(7, "T", "C", "mario", "");
        n.setPermesso(new Scrittura());
        n.getUtentiCondivisi().add("anna");

        when(repo.findById(7)).thenReturn(n);

        service.restoreVersion(7, 0, "anna");

        verify(repo).restoreVersion(7, 0, "anna");
    }

    @Test
    void testRestoreVersionForbiddenForLettura() {
        Note n = new Note(7, "T", "C", "mario", "");
        n.setPermesso(new Lettura());
        n.getUtentiCondivisi().add("anna");

        when(repo.findById(7)).thenReturn(n);

        assertThrows(RuntimeException.class,
                () -> service.restoreVersion(7, 0, "anna"));
    }
}
