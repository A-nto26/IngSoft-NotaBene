package com.sweng.notes.service;

import com.sweng.notes.model.Cartella;
import com.sweng.notes.model.Note;
import com.sweng.notes.repository.NoteRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FolderServiceTest {

    private NoteRepository noteRepo;
    private FolderService folderService;

    @BeforeEach
    void setup() {
        noteRepo = mock(NoteRepository.class);
        folderService = new FolderService(noteRepo);
    }

    // ============================================================
    // GET ALL FOLDERS
    // ============================================================

    @Test
    void testGetAllFolders_ok() {
        when(noteRepo.findAllFolders()).thenReturn(List.of(
                new Cartella("casa", "mario", "#fff")
        ));

        List<Cartella> res = folderService.getAllFolders();

        assertNotNull(res);
        assertEquals(1, res.size());
        verify(noteRepo).findAllFolders();
    }

    // ============================================================
    // GET NOTES IN FOLDER
    // ============================================================

    @Test
    void testGetNotesInFolder_ok_normalizzaNome() {
        when(noteRepo.findByCartella("casa"))
                .thenReturn(List.of(new Note(1, "T", "C", "mario", "casa")));

        List<Note> res = folderService.getNotesInFolder("  CASA  ");

        assertNotNull(res);
        assertEquals(1, res.size());
        verify(noteRepo).findByCartella("casa");
    }

    @Test
    void testGetNotesInFolder_nomeVuoto_restituisceVuoto() {
        List<Note> res = folderService.getNotesInFolder("   ");

        assertNotNull(res);
        assertTrue(res.isEmpty());
        verify(noteRepo, never()).findByCartella(anyString());
    }

    // ============================================================
    // CREATE FOLDER
    // ============================================================

    @Test
    void testCreateFolder_ok_conColore() {
        when(noteRepo.findFolderByName("casa")).thenReturn(null);

        folderService.createFolder(" CASA ", "#123456", " MARIO ");

        verify(noteRepo).createFolder("casa", "mario", "#123456");
    }

    @Test
    void testCreateFolder_ok_defaultColor() {
        when(noteRepo.findFolderByName("progetti")).thenReturn(null);

        folderService.createFolder(" Progetti ", "  ", " Luca ");

        verify(noteRepo).createFolder("progetti", "luca", "#FFD700");
    }

    @Test
    void testCreateFolder_nomeMancante_lanciaEccezione() {
        assertThrows(IllegalArgumentException.class,
                () -> folderService.createFolder("   ", "#fff", "mario"));

        verifyNoInteractions(noteRepo);
    }

    @Test
    void testCreateFolder_cartellaGiaEsistente_lanciaEccezione() {
        when(noteRepo.findFolderByName("casa")).thenReturn(new Cartella("casa", "mario", "#fff"));

        assertThrows(IllegalArgumentException.class,
                () -> folderService.createFolder("casa", "#000", "mario"));

        verify(noteRepo).findFolderByName("casa");
        verify(noteRepo, never()).createFolder(anyString(), anyString(), anyString());
    }

    // ============================================================
    // DELETE FOLDER
    // ============================================================

    @Test
    void testDeleteFolder_ok() {
        folderService.deleteFolder(" CASA ");

        verify(noteRepo).deleteFolder("casa");
    }

    @Test
    void testDeleteFolder_nomeVuoto_nonFaNulla() {
        folderService.deleteFolder("   ");

        verify(noteRepo, never()).deleteFolder(anyString());
    }

    // ============================================================
    // GET NOTES IN FOLDER FOR USER
    // ============================================================

    @Test
    void testGetNotesInFolderForUser_ok_filtraPuoLeggere() {
        Note leggibile = mock(Note.class);
        when(leggibile.puoLeggere("mario")).thenReturn(true);

        Note nonLeggibile = mock(Note.class);
        when(nonLeggibile.puoLeggere("mario")).thenReturn(false);

        when(noteRepo.findByCartella("casa"))
                .thenReturn(List.of(leggibile, nonLeggibile));

        List<Note> res = folderService.getNotesInFolderForUser(" CASA ", " MARIO ");

        assertNotNull(res);
        assertEquals(1, res.size());
        assertSame(leggibile, res.get(0));

        verify(noteRepo).findByCartella("casa");
        verify(leggibile).puoLeggere("mario");
        verify(nonLeggibile).puoLeggere("mario");
    }

    @Test
    void testGetNotesInFolderForUser_parametriInvalidi_restituisceVuoto() {
        List<Note> res1 = folderService.getNotesInFolderForUser("  ", "mario");
        List<Note> res2 = folderService.getNotesInFolderForUser("casa", "  ");

        assertEquals(Collections.emptyList(), res1);
        assertEquals(Collections.emptyList(), res2);

        verify(noteRepo, never()).findByCartella(anyString());
    }
}
