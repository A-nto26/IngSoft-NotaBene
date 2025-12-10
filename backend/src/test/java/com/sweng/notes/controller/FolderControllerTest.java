package com.sweng.notes.controller;

import com.sweng.notes.dto.FolderRequest;
import com.sweng.notes.model.Cartella;
import com.sweng.notes.model.Note;
import com.sweng.notes.service.FolderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FolderControllerTest {

    private FolderService folderService;
    private FolderController controller;

    @BeforeEach
    void setup() {
        folderService = mock(FolderService.class);
        controller = new FolderController(folderService);
    }

    // ============================================================
    // GET ALL FOLDERS
    // ============================================================
    @Test
    void testGetAllFolders() {
        List<Cartella> mockList = List.of(
                new Cartella("casa", "mario", "#fff"),
                new Cartella("studio", "anna", "#000"));

        when(folderService.getAllFolders()).thenReturn(mockList);

        ResponseEntity<List<Cartella>> res = controller.getAllFolders();

        assertEquals(200, res.getStatusCode().value());
        assertEquals(2, res.getBody().size());
        verify(folderService, times(1)).getAllFolders();
    }

 /*
    // ============================================================
    // GET NOTES IN FOLDER (endpoint non piu attivo)
    // manteniamo per completezza
    // ============================================================
    @Test
    void testGetNotesInFolder() {
        List<Note> mockNotes = List.of(
                new Note(1, "Titolo1", "C1", "mario", "casa"),
                new Note(2, "Titolo2", "C2", "mario", "casa"));

        when(folderService.getNotesInFolder("casa")).thenReturn(mockNotes);

        ResponseEntity<List<Note>> res = controller.getNotesInFolder("casa");

        assertEquals(200, res.getStatusCode().value());
        assertEquals(2, res.getBody().size());
    }
*/
    // ============================================================
    // CREATE FOLDER
    // ============================================================
    @Test
    void testCreateFolderSuccess() {
        FolderRequest req = new FolderRequest();
        req.setNome("casa");
        req.setCreatore("mario");
        req.setColore("#123456");

        ResponseEntity<String> res = controller.createFolder(req);

        assertEquals(200, res.getStatusCode().value());
        assertTrue(res.getBody().contains("casa"));

        verify(folderService, times(1))
                .createFolder("casa", "#123456", "mario");
    }

    @Test
    void testCreateFolderFailMissingName() {
        FolderRequest req = new FolderRequest();
        req.setNome("");

        ResponseEntity<String> res = controller.createFolder(req);

        assertEquals(400, res.getStatusCode().value());
        verify(folderService, never()).createFolder(any(), any(), any());
    }

    // ============================================================
    // DELETE FOLDER
    // ============================================================
    @Test
    void testDeleteFolder() {
        ResponseEntity<String> res = controller.deleteFolder("casa");

        assertEquals(200, res.getStatusCode().value());
        verify(folderService, times(1)).deleteFolder("casa");
    }

    // ============================================================
    // GET NOTES IN FOLDER FOR USER (Sprint 4: aggiunti bad request)
    // ============================================================
    @Test
    void testGetNotesInFolderForUser() {
        List<Note> mockNotes = List.of(
                new Note(1, "A", "B", "mario", "casa"));

        when(folderService.getNotesInFolderForUser("casa", "mario"))
                .thenReturn(mockNotes);

        ResponseEntity<List<Note>> res = controller.getNotesInFolderForUser("casa", "mario");

        assertEquals(200, res.getStatusCode().value());
        assertEquals(1, res.getBody().size());
        verify(folderService).getNotesInFolderForUser("casa", "mario");
    }

    // === NUOVI TEST SPRINT 4 ===

    @Test
    void testGetNotesInFolderForUser_BadRequest_EmptyFolder() {
        ResponseEntity<List<Note>> res = controller.getNotesInFolderForUser("", "mario");

        assertEquals(400, res.getStatusCode().value());
        verify(folderService, never()).getNotesInFolderForUser(any(), any());
    }

    @Test
    void testGetNotesInFolderForUser_BadRequest_EmptyUser() {
        ResponseEntity<List<Note>> res = controller.getNotesInFolderForUser("casa", "");

        assertEquals(400, res.getStatusCode().value());
        verify(folderService, never()).getNotesInFolderForUser(any(), any());
    }
}