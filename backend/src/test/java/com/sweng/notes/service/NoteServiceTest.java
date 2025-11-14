package com.sweng.notes.service;

import com.sweng.notes.model.Note;
import com.sweng.notes.repository.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class NoteServiceTest {

    private NoteRepository noteRepo;
    private NoteService noteService;

    @BeforeEach
    void setup() {
        noteRepo = Mockito.mock(NoteRepository.class);
        noteService = new NoteService(noteRepo);
    }

    @Test
    void testCreateNote() {
        Note mockNote = new Note(1, "Titolo", "Contenuto", "mario");

        when(noteRepo.save("Titolo", "Contenuto", "mario")).thenReturn(mockNote);

        Note result = noteService.create("Titolo", "Contenuto", "mario");

        assertNotNull(result);
        assertEquals("Titolo", result.getTitolo());
        verify(noteRepo, times(1)).save("Titolo", "Contenuto", "mario");
    }

    @Test
    void testGetByUserReturnsList() {
        when(noteRepo.findByCreatore("anna"))
                .thenReturn(List.of(new Note(1, "A", "B", "anna")));

        List<Note> result = noteService.getByUser("anna");

        assertEquals(1, result.size());
        assertEquals("anna", result.get(0).getCreatore());
    }
}
