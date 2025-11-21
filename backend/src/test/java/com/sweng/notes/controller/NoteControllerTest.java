package com.sweng.notes.controller;

import com.sweng.notes.model.Note;
import com.sweng.notes.service.NoteService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NoteControllerTest {

    @Test
    void testCreateNote() {
        NoteService service = new NoteService(new com.sweng.notes.repository.NoteRepository());
        NoteController controller = new NoteController(service);

        ResponseEntity<Note> res = controller.create("Titolo", "Contenuto", "user1", "casa");

        assertEquals(200, res.getStatusCode().value());
        assertEquals("Titolo", res.getBody().getTitolo());
    }

    @Test
    void testGetByUser() {
        NoteService service = new NoteService(new com.sweng.notes.repository.NoteRepository());
        NoteController controller = new NoteController(service);

        controller.create("A", "B", "user1", "");
        controller.create("C", "D", "user2", "");

        ResponseEntity<List<Note>> res = controller.getByUser("user1");

        assertEquals(1, res.getBody().size());
    }

    @Test
    void testDeleteNote() {
        NoteService service = new NoteService(new com.sweng.notes.repository.NoteRepository());
        NoteController controller = new NoteController(service);

        Note n = controller.create("Titolo", "Contenuto", "user1", "").getBody();
        controller.delete(n.getId());

        assertTrue(controller.getByUser("user1").getBody().isEmpty());
    }
}