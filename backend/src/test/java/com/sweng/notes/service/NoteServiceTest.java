package com.sweng.notes.service;

import com.sweng.notes.model.Note;
import com.sweng.notes.repository.NoteRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NoteServiceTest {

    @Test
    void testCreateNote() {
        NoteRepository repo = new NoteRepository();
        NoteService service = new NoteService(repo);

        Note n = service.create("Titolo", "Contenuto", "user1", "casa");

        assertNotNull(n);
        assertEquals("Titolo", n.getTitolo());
        assertEquals("casa", n.getCartella());
    }

    @Test
    void testUpdateNote() {
        NoteRepository repo = new NoteRepository();
        NoteService service = new NoteService(repo);

        Note n = service.create("T1", "C1", "user1", "");

        service.update(n.getId(), "T2", "C2");

        Note updated = repo.findById(n.getId());
        assertEquals("T2", updated.getTitolo());
        assertEquals("C2", updated.getContenuto());
    }

    @Test
    void testDuplicateNote() {
        NoteRepository repo = new NoteRepository();
        NoteService service = new NoteService(repo);

        Note n = service.create("Originale", "Test", "user1", "casa");

        Note copia = service.duplicate(n.getId(), "user2");

        assertNotNull(copia);
        assertNotEquals(n.getId(), copia.getId());
        assertEquals("Originale (Copia)", copia.getTitolo());
        assertEquals("user2", copia.getCreatore());
    }

    @Test
    void testSearch() {
        NoteRepository repo = new NoteRepository();
        NoteService service = new NoteService(repo);

        service.create("Fare la spesa", "Compra latte", "user1", "");
        service.create("Lavoro", "Preparare slides", "user1", "");

        List<Note> result = service.search("user1", "latte");
        assertEquals(1, result.size());
    }
}