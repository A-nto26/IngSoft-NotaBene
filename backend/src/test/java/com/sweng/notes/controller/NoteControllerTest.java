package com.sweng.notes.controller;

import com.sweng.notes.dto.CreateNoteRequest;
import com.sweng.notes.dto.NoteUpdateRequest;
import com.sweng.notes.dto.NoteView;
import com.sweng.notes.dto.ShareNoteRequest;
import com.sweng.notes.model.Note;
import com.sweng.notes.service.NoteService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NoteControllerTest {

    private NoteService noteService;
    private NoteController controller;

    @BeforeEach
    void setup() {
        noteService = mock(NoteService.class);
        controller = new NoteController(noteService);
    }

    // ============================================================
    // CREATE NOTE
    // ============================================================
    @Test
    void testCreateNote_ok() {
        CreateNoteRequest req = new CreateNoteRequest();
        req.setTitolo("T");
        req.setContenuto("C");
        req.setCreatore("Mario"); 
        
        when(noteService.create(any(CreateNoteRequest.class)))
                .thenReturn(new Note(1, "T", "C", "mario", null));

        ResponseEntity<String> res = controller.createNote(req);

        assertEquals(200, res.getStatusCode().value());
        assertNotNull(res.getBody());

        // Verifico che il service venga chiamato (il controller modifica req.setCreatore(...))
        verify(noteService).create(req);
        assertEquals("mario", req.getCreatore());
    }

    @Test
    void testCreateNote_badRequest_missingFields() {
        CreateNoteRequest req = new CreateNoteRequest();
        req.setTitolo(""); // titolo vuoto

        ResponseEntity<String> res = controller.createNote(req);

        assertEquals(400, res.getStatusCode().value());
        verifyNoInteractions(noteService);
    }

    // ============================================================
    // GET VISIBLE
    // ============================================================
    @Test
    void testGetVisible_ok() {
        when(noteService.getVisibleNotesForUser("mario"))
                .thenReturn(List.of(new Note(1, "T", "C", "mario", null)));

        when(noteService.toView(any(Note.class), eq("mario")))
                .thenReturn(new NoteView());

        ResponseEntity<List<NoteView>> res = controller.getVisible("Mario");

        assertEquals(200, res.getStatusCode().value());
        assertNotNull(res.getBody());
        assertEquals(1, res.getBody().size());

        // importante: il controller normalizza "Mario" -> "mario"
        verify(noteService).getVisibleNotesForUser("mario");
    }

    // ============================================================
    // DELETE NOTE
    // ============================================================
    @Test
    void testDeleteNote_ok() {
        Note n = new Note(3, "T", "C", "mario", null);
        when(noteService.getNoteById(3)).thenReturn(n);

        ResponseEntity<String> res = controller.deleteNote(3, "Mario");

        assertEquals(200, res.getStatusCode().value());
        verify(noteService).delete(3, "mario"); // normalizzato
    }

    // ============================================================
    // REMOVE SELF
    // ============================================================
    @Test
    void testRemoveSelf_ok() {
        Note n = new Note(2, "T", "C", "pippo", null);
        n.getUtentiCondivisi().add("anna");
        when(noteService.getNoteById(2)).thenReturn(n);

        ResponseEntity<String> res = controller.removeSelf(2, Map.of("user", "ANNA"));

        assertEquals(200, res.getStatusCode().value());
        verify(noteService).removeSelf(2, "anna"); // normalizzato
    }

    // ============================================================
    // DUPLICATE
    // ============================================================
    @Test
    void testDuplicate_ok() {
        when(noteService.duplicate(1, "mario"))
                .thenReturn(new Note(9, "T (Copia)", "C", "mario", null));

        ResponseEntity<Note> res = controller.duplicateNote(1, "Mario");

        assertEquals(200, res.getStatusCode().value());
        assertNotNull(res.getBody());
        verify(noteService).duplicate(1, "mario"); // normalizzato
    }

    // ============================================================
    // LOCK
    // ============================================================
    @Test
    void testLock_ok() {
        when(noteService.lock(5, "mario")).thenReturn("locked");

        ResponseEntity<?> res = controller.lockNote(5, "Mario");

        assertEquals(200, res.getStatusCode().value());
        verify(noteService).lock(5, "mario");
    }

    // ============================================================
    // UPDATE NOTE
    // ============================================================
    @Test
    void testUpdateNote_ok() {
        // Nota mockata per gestire puoScrivere()
        Note n = mock(Note.class);
        when(noteService.getNoteById(5)).thenReturn(n);

        when(n.getCreatore()).thenReturn("mario");
        when(n.puoScrivere("mario")).thenReturn(true);

        // serve per versioneCorrente = versioni.size()+1
        when(n.getVersioni()).thenReturn(new ArrayList<>());

        // lock valido e detenuto dallo stesso utente
        when(noteService.getLockOwner(5)).thenReturn(Optional.of("mario"));

        NoteUpdateRequest req = new NoteUpdateRequest();
        req.setTitolo("New");
        req.setContenuto("NewC");
        req.setVersionExpected(1);

        ResponseEntity<String> res = controller.updateNote(5, "Mario", req);

        assertEquals(200, res.getStatusCode().value());
        verify(noteService).update(5, req, "mario");
        verify(noteService).unlock(5, "mario");
    }

    // ============================================================
    // SHARE
    // ============================================================
    @Test
    void testShare_ok() {
        Note n = new Note(1, "T", "C", "mario", null);
        when(noteService.getNoteById(1)).thenReturn(n);

        ShareNoteRequest req = new ShareNoteRequest();
        req.setUtentiCondivisi(List.of("ANNA"));

        ResponseEntity<String> res = controller.share(1, req, "Mario");

        assertEquals(200, res.getStatusCode().value());
        verify(noteService).shareNote(eq(1), any(ShareNoteRequest.class), eq("mario"));
    }
}
