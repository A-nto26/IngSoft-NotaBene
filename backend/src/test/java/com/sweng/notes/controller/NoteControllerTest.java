package com.sweng.notes.controller;

import com.sweng.notes.model.Note;
import com.sweng.notes.service.NoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class NoteControllerTest {

    private NoteService noteService;
    private NoteController controller;

    @BeforeEach
    void setup() {
        noteService = Mockito.mock(NoteService.class);
        controller = new NoteController(noteService);
    }

    @Test
    void testCreateNote() {
        Note mock = new Note(1, "T", "C", "mario");
        when(noteService.create("T", "C", "mario")).thenReturn(mock);

        ResponseEntity<Note> response = controller.create("T", "C", "mario");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("T", response.getBody().getTitolo());
    }

    @Test
    void testGetByUser() {
        when(noteService.getByUser("anna"))
                .thenReturn(List.of(new Note(1, "A", "B", "anna")));

        ResponseEntity<List<Note>> response = controller.getByUser("anna");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("anna", response.getBody().get(0).getCreatore());
    }
}
