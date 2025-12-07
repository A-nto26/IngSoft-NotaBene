package com.sweng.notes.service;

import com.sweng.notes.model.Cartella;
import com.sweng.notes.model.Note;
import com.sweng.notes.model.Lettura;
import com.sweng.notes.model.Scrittura;
import com.sweng.notes.repository.NoteRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

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
    void testGetAllFolders() {
        List<Cartella> mockList = List.of(
                new Cartella("casa", "mario", "#fff"),
                new Cartella("studio", "anna", "#000"));

        when(noteRepo.findAllFolders()).thenReturn(mockList);

        List<Cartella> result = folderService.getAllFolders();

        assertEquals(2, result.size());
        verify(noteRepo).findAllFolders();
    }

    // ============================================================
    // GET NOTES IN FOLDER
    // ============================================================
    @Test
    void testGetNotesInFolder() {
        List<Note> mockNotes = List.of(
                new Note(1, "T1", "C1", "mario", "casa"),
                new Note(2, "T2", "C2", "mario", "casa"));

        when(noteRepo.findByCartella("casa")).thenReturn(mockNotes);

        List<Note> result = folderService.getNotesInFolder("casa");

        assertEquals(2, result.size());
        verify(noteRepo).findByCartella("casa");
    }

    @Test
    void testGetNotesInFolderEmptyName() {
        List<Note> result = folderService.getNotesInFolder("  ");

        assertTrue(result.isEmpty());
        verify(noteRepo, never()).findByCartella(any());
    }

    // ============================================================
    // CREATE FOLDER
    // ============================================================
    @Test
    void testCreateFolderSuccess() {
        folderService.createFolder("casa", "#123456", "mario");

        verify(noteRepo).createFolder("casa", "mario", "#123456");
    }

    @Test
    void testCreateFolderFailMissingName() {
        assertThrows(IllegalArgumentException.class,
                () -> folderService.createFolder(" ", "#fff", "mario"));

        verify(noteRepo, never()).createFolder(any(), any(), any());
    }

    @Test
    void testCreateFolderDefaultColor() {
        folderService.createFolder("progetti", null, "luca");

        verify(noteRepo).createFolder("progetti", "luca", "#FFD700");
    }

    @Test
    void testCreateFolderNameNormalization() {
        folderService.createFolder("   LAVORO  ", "#111111", "gigi");

        verify(noteRepo).createFolder("lavoro", "gigi", "#111111");
    }

    // Cartella duplicata -> repo lancia IllegalArgumentException
    @Test
    void testCreateFolderAlreadyExists() {
        doThrow(new IllegalArgumentException("duplicate"))
                .when(noteRepo).createFolder("casa", "mario", "#000");

        assertThrows(IllegalArgumentException.class,
                () -> folderService.createFolder("casa", "#000", "mario"));
    }

    // ============================================================
    // DELETE FOLDER
    // ============================================================
    @Test
    void testDeleteFolder() {
        folderService.deleteFolder("casa");
        verify(noteRepo).deleteFolder("casa");
    }

    @Test
    void testDeleteFolderEmptyName() {
        folderService.deleteFolder("  ");
        verify(noteRepo, never()).deleteFolder(any());
    }

    // ============================================================
    // GET VISIBLE NOTES FOR USER
    // ============================================================

    @Test
    void testGetNotesInFolderForUserCreator() {
        List<Note> mockNotes = List.of(
                new Note(1, "A", "B", "mario", "casa"),
                new Note(2, "C", "D", "anna", "casa"));

        when(noteRepo.findByCartella("casa")).thenReturn(mockNotes);

        List<Note> result = folderService.getNotesInFolderForUser("casa", "mario");

        assertEquals(1, result.size());
        assertEquals("mario", result.get(0).getCreatore());
    }

    @Test
    void testGetNotesInFolderForUserSharedLettura() {
        Note shared = new Note(1, "A", "B", "mario", "casa");
        shared.getUtentiCondivisi().add("anna");
        shared.setPermesso(new Lettura());

        when(noteRepo.findByCartella("casa")).thenReturn(List.of(shared));

        List<Note> result = folderService.getNotesInFolderForUser("casa", "anna");

        assertEquals(1, result.size());
        assertEquals("A", result.get(0).getTitolo());
    }

    @Test
    void testGetNotesInFolderForUserSharedScrittura() {
        Note shared = new Note(1, "A", "B", "mario", "casa");
        shared.getUtentiCondivisi().add("anna");
        shared.setPermesso(new Scrittura());

        when(noteRepo.findByCartella("casa")).thenReturn(List.of(shared));

        List<Note> result = folderService.getNotesInFolderForUser("casa", "anna");

        assertEquals(1, result.size());
        assertEquals("A", result.get(0).getTitolo());
    }

    @Test
    void testGetNotesInFolderForUserNoPermission() {
        Note shared = new Note(1, "A", "B", "mario", "casa");
        shared.getUtentiCondivisi().add("anna");
        shared.setPermesso(null); // permesso nullo = privata

        when(noteRepo.findByCartella("casa")).thenReturn(List.of(shared));

        List<Note> result = folderService.getNotesInFolderForUser("casa", "anna");

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetNotesInFolderForUserEmptyParams() {
        assertTrue(folderService.getNotesInFolderForUser(" ", "mario").isEmpty());
        assertTrue(folderService.getNotesInFolderForUser("casa", " ").isEmpty());
    }
}