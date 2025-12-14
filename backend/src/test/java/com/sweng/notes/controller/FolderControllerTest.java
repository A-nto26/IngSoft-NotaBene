package com.sweng.notes.controller;

import com.sweng.notes.dto.FolderRequest;
import com.sweng.notes.dto.FolderResponse;
import com.sweng.notes.model.Cartella;
import com.sweng.notes.model.Note;
import com.sweng.notes.service.FolderService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
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
    void testGetAllFolders_ok() {
        when(folderService.getAllFolders()).thenReturn(List.of(
                new Cartella("casa", "mario", "#fff")
        ));

        ResponseEntity<List<FolderResponse>> res = controller.getAllFolders();

        assertEquals(200, res.getStatusCode().value());
        assertNotNull(res.getBody());
        assertEquals(1, res.getBody().size());
        assertEquals("casa", res.getBody().get(0).getNome());

        verify(folderService).getAllFolders();
    }

    // ============================================================
    // CREATE FOLDER
    // ============================================================
    @Test
    void testCreateFolder_ok() {
        FolderRequest req = new FolderRequest();
        req.setNome("casa");
        req.setCreatore("mario");
        req.setColore("#123456");

        ResponseEntity<String> res = controller.createFolder(req);

        assertEquals(200, res.getStatusCode().value());
        assertNotNull(res.getBody());

        verify(folderService).createFolder("casa", "#123456", "mario");
    }

    @Test
    void testCreateFolder_badRequest_nomeVuoto() {
        FolderRequest req = new FolderRequest();
        req.setNome("");

        ResponseEntity<String> res = controller.createFolder(req);

        assertEquals(400, res.getStatusCode().value());
        verifyNoInteractions(folderService);
    }

    @Test
    void testCreateFolder_conflict_cartellaGiaEsistente() {
        FolderRequest req = new FolderRequest();
        req.setNome("casa");
        req.setCreatore("mario");
        req.setColore("#123456");

        doThrow(new IllegalArgumentException("La cartella 'casa' esiste già."))
                .when(folderService).createFolder("casa", "#123456", "mario");

        ResponseEntity<String> res = controller.createFolder(req);

        assertEquals(409, res.getStatusCode().value());
        assertNotNull(res.getBody());

        verify(folderService).createFolder("casa", "#123456", "mario");
    }

    // ============================================================
    // DELETE FOLDER
    // ============================================================
    @Test
    void testDeleteFolder_ok() {
        ResponseEntity<String> res = controller.deleteFolder("casa");

        assertEquals(200, res.getStatusCode().value());
        verify(folderService).deleteFolder("casa");
    }

    // ============================================================
    // GET NOTES IN FOLDER FOR USER
    // ============================================================
    @Test
    void testGetNotesInFolderForUser_ok() {
        when(folderService.getNotesInFolderForUser("casa", "mario"))
                .thenReturn(List.of(new Note(1, "T", "C", "mario", "casa")));

        ResponseEntity<List<Note>> res = controller.getNotesInFolderForUser("casa", "mario");

        assertEquals(200, res.getStatusCode().value());
        assertNotNull(res.getBody());
        assertEquals(1, res.getBody().size());

        verify(folderService).getNotesInFolderForUser("casa", "mario");
    }

    @Test
    void testGetNotesInFolderForUser_badRequest_paramVuoti() {
        ResponseEntity<List<Note>> res1 = controller.getNotesInFolderForUser("", "mario");
        assertEquals(400, res1.getStatusCode().value());

        ResponseEntity<List<Note>> res2 = controller.getNotesInFolderForUser("casa", "");
        assertEquals(400, res2.getStatusCode().value());

        verify(folderService, never()).getNotesInFolderForUser(any(), any());
    }
}
