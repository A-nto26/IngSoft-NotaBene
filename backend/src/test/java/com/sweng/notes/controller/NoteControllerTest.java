package com.sweng.notes.controller;

import com.sweng.notes.dto.*;
import com.sweng.notes.model.Note;
import com.sweng.notes.model.VersioneNota;
import com.sweng.notes.service.NoteService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class NoteControllerTest {

    private NoteService service;
    private NoteController controller;

    @BeforeEach
    void setup() {
        service = mock(NoteService.class);
        controller = new NoteController(service);
    }

    // ============================================================
    // CREATE NOTE
    // ============================================================
    @Test
    void testCreateNote() {
        CreateNoteRequest req = new CreateNoteRequest();
        req.setTitolo("Titolo");
        req.setContenuto("Contenuto");
        req.setCreatore("mario");
        req.setCartella("casa");

        when(service.create(req)).thenReturn(new Note(1, "Titolo", "Contenuto", "mario", "casa"));

        ResponseEntity<String> res = controller.createNote(req);

        assertEquals(200, res.getStatusCode().value());
        verify(service).create(req);
    }

    // ============================================================
    // GET VISIBLE NOTES (Nota: ora restituisce NoteView)
    // ============================================================
    @Test
    void testGetVisibleNotes() {
        List<Note> fakeNotes = List.of(new Note(1, "A", "B", "mario", ""));

        when(service.getVisibleNotesForUser("mario")).thenReturn(fakeNotes);
        when(service.toView(any(), eq("mario")))
                .thenReturn(new NoteView());

        ResponseEntity<List<NoteView>> res = controller.getVisible("mario");

        assertEquals(1, res.getBody().size());
        verify(service).getVisibleNotesForUser("mario");
    }

    // ============================================================
    // UPDATE NOTE
    // ============================================================
    @Test
    void testUpdateNote() {
        Note n = new Note(5, "Old", "C", "mario", "");
        n.setVersioni(new ArrayList<>());

        when(service.getNoteById(5)).thenReturn(n);
        when(service.getLockOwner(5)).thenReturn(Optional.of("mario"));

        NoteUpdateRequest req = new NoteUpdateRequest();
        req.setTitolo("Nuovo");
        req.setContenuto("Aggiornato");
        req.setVersionExpected(1);

        ResponseEntity<String> res = controller.updateNote(5, "mario", req);

        assertEquals(200, res.getStatusCode().value());
        verify(service).update(5, req, "mario");
        verify(service).unlock(5, "mario");
    }

    // ============================================================
    // DELETE NOTE
    // ============================================================
    @Test
    void testDeleteNote() {
        Note n = new Note(3, "T", "C", "mario", "");
        when(service.getNoteById(3)).thenReturn(n);

        ResponseEntity<String> res = controller.deleteNote(3, "mario");

        assertEquals(200, res.getStatusCode().value());
        verify(service).delete(3, "mario");
    }

    // ============================================================
    // RESTORE VERSION
    // ============================================================
    @Test
    void testRestoreVersion() {
        Note n = new Note(10, "T", "C", "mario", "");

        VersioneNota v = new VersioneNota();
        v.setTitolo("v1");
        v.setContenuto("c1");

        n.setVersioni(List.of(v));

        when(service.getNoteById(10)).thenReturn(n);
        when(service.getLockOwner(10)).thenReturn(Optional.of("mario"));

        ResponseEntity<String> res = controller.restoreVersion(10, 0, "mario");

        assertEquals(200, res.getStatusCode().value());
        verify(service).restoreVersion(10, 0, "mario");
    }

    // ============================================================
    // SHARE NOTE
    // ============================================================
    @Test
    void testShareNote() {
        Note n = new Note(1, "T", "C", "mario", "");
        when(service.getNoteById(1)).thenReturn(n);

        ShareNoteRequest req = new ShareNoteRequest();
        req.setUtentiCondivisi(List.of("anna", "luca"));

        ResponseEntity<String> res = controller.share(1, req, "mario");

        assertEquals(200, res.getStatusCode().value());
        verify(service).shareNote(1, req, "mario");
    }

    // ============================================================
    // REMOVE SELF (ENDPOINT /{id}/removeSelf)
    // ============================================================
    @Test
    void testRemoveSelf() {
        Map<String, String> body = Map.of("user", "anna");

        Note n = new Note(2, "T", "C", "pippo", "");
        n.setUtentiCondivisi(new HashSet<>(List.of("anna")));

        when(service.getNoteById(2)).thenReturn(n);

        ResponseEntity<String> res = controller.removeSelf(2, body);

        assertEquals(200, res.getStatusCode().value());
        verify(service).removeSelf(2, "anna");
    }

    // ============================================================
    // DUPLICATE NOTE
    // ============================================================
    @Test
    void testDuplicateNote() {
        Note fake = new Note(2, "Copiata", "C", "luca", "");
        when(service.duplicate(1, "luca")).thenReturn(fake);

        ResponseEntity<Note> res = controller.duplicateNote(1, "luca");

        assertEquals("Copiata", res.getBody().getTitolo());
        verify(service).duplicate(1, "luca");
    }

    // ============================================================
    // LOCK NOTE
    // ============================================================
    @Test
    void testLockNote() {
        when(service.lock(5, "mario")).thenReturn("locked");

        ResponseEntity<?> res = controller.lockNote(5, "mario");

        assertEquals(200, res.getStatusCode().value());
        verify(service).lock(5, "mario");
    }

    @Test
    void testUnlockNote() {
        when(service.unlock(5, "mario")).thenReturn("unlocked");

        ResponseEntity<?> res = controller.unlockNote(5, "mario");

        assertEquals(200, res.getStatusCode().value());
        verify(service).unlock(5, "mario");
    }

    @SuppressWarnings("unchecked")
    @Test
    void testGetLockState() {
        Map<String, Object> mockState = Map.of(
                "locked", true,
                "lockedBy", "anna");

        when(service.getLockState(8)).thenReturn(mockState);

        ResponseEntity<?> res = controller.getLockState(8);

        Map<String, Object> body = (Map<String, Object>) res.getBody();

        assertEquals(true, body.get("locked"));
        assertEquals("anna", body.get("lockedBy"));

        verify(service).getLockState(8);
    }
}
