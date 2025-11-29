package com.sweng.notes.controller;

import com.sweng.notes.dto.*;
import com.sweng.notes.model.Note;
import com.sweng.notes.service.NoteService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

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
    // CREATE — Sprint 3
    // ============================================================
    @Test
    void testCreateNote() {
        CreateNoteRequest req = new CreateNoteRequest();
        req.setTitolo("Titolo");
        req.setContenuto("Contenuto");
        req.setCreatore("mario");
        req.setCartella("casa");
        req.setPermesso("PRIVATA");
        req.setUtentiCondivisi(List.of("anna"));

        Note fake = new Note(1, "Titolo", "Contenuto", "mario", "casa");

        when(service.create(req)).thenReturn(fake);

        ResponseEntity<Note> res = controller.create(req);

        assertEquals(200, res.getStatusCode().value());
        assertEquals("Titolo", res.getBody().getTitolo());
        verify(service).create(req);
    }

    // ============================================================
    // GET VISIBLE NOTES
    // ============================================================
    @Test
    void testGetVisibleNotes() {
        List<Note> mockList = List.of(new Note(1, "A", "B", "mario", "casa"));

        when(service.getVisibleNotes("mario")).thenReturn(mockList);

        ResponseEntity<List<Note>> res = controller.getVisibleNotes("mario");

        assertEquals(1, res.getBody().size());
        verify(service).getVisibleNotes("mario");
    }

    // ============================================================
    // UPDATE — Sprint 3
    // ============================================================
    @Test
    void testUpdateNote() {
        NoteUpdateRequest req = new NoteUpdateRequest();
        req.setTitolo("Nuovo");
        req.setContenuto("Aggiornato");

        ResponseEntity<Void> res = controller.update(5, "mario", req);

        assertEquals(200, res.getStatusCode().value());
        verify(service).update(5, req, "mario");
    }

    // ============================================================
    // DUPLICATE
    // ============================================================
    @Test
    void testDuplicateNote() {
        Note fake = new Note(2, "Copiata", "C", "luca", "casa");

        when(service.duplicate(1, "luca")).thenReturn(fake);

        ResponseEntity<Note> res = controller.duplicate(1, "luca");

        assertEquals(200, res.getStatusCode().value());
        assertEquals("Copiata", res.getBody().getTitolo());
        verify(service).duplicate(1, "luca");
    }

    // ============================================================
    // DELETE
    // ============================================================
    @Test
    void testDeleteNote() {
        ResponseEntity<Void> res = controller.delete(3, "mario");

        assertEquals(200, res.getStatusCode().value());
        verify(service).delete(3, "mario");
    }

    // ============================================================
    // SEARCH
    // ============================================================
    @Test
    void testSearchNotes() {
        List<Note> mockList = List.of(new Note(1, "A", "B", "mario", ""));

        when(service.search("mario", "ciao")).thenReturn(mockList);

        ResponseEntity<List<Note>> res = controller.search("mario", "ciao");

        assertEquals(1, res.getBody().size());
        verify(service).search("mario", "ciao");
    }

    // ============================================================
    // SET FOLDER — Sprint 3
    // ============================================================
    @Test
    void testSetFolder() {
        FolderRequest req = new FolderRequest();
        req.setNome("casa");

        ResponseEntity<Void> res = controller.setFolder(7, "mario", req);

        assertEquals(200, res.getStatusCode().value());
        verify(service).setCartella(7, req.getNome(), "mario");
    }

    // ============================================================
    // SHARE — Sprint 3
    // ============================================================
    @Test
    void testShareNote() {
        ShareNoteRequest req = new ShareNoteRequest();
        req.setUtentiCondivisi(List.of("anna", "luca"));

        ResponseEntity<Void> res = controller.share(1, "mario", req);

        assertEquals(200, res.getStatusCode().value());
        verify(service).shareNote(1, req, "mario");
    }

    // ============================================================
    // REMOVE SELF FROM SHARE
    // ============================================================
    @Test
    void testRemoveSelf() {
        UserRequest req = new UserRequest("anna", null);

        ResponseEntity<Void> res = controller.removeSelf(2, req);

        assertEquals(200, res.getStatusCode().value());
        verify(service).removeSelf(2, "anna");
    }

    // ============================================================
    // RESTORE VERSION
    // ============================================================
    @Test
    void testRestoreVersion() {
        ResponseEntity<Void> res = controller.restoreVersion(10, 3, "mario");

        assertEquals(200, res.getStatusCode().value());
        verify(service).restoreVersion(10, 3, "mario");
    }
}
