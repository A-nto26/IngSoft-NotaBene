package com.sweng.notes.repository;

import com.sweng.notes.model.Note;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NoteRepositoryTest {

    @Test
    void testSaveAndFindById() {
        NoteRepository repo = new NoteRepository();

        Note n = new Note(0, "Titolo", "Contenuto", "user1", "lavoro");
        repo.save(n);

        Note found = repo.findById(n.getId());
        assertNotNull(found);
        assertEquals("Titolo", found.getTitolo());
    }

    @Test
    void testFindByCreatore() {
        NoteRepository repo = new NoteRepository();

        repo.save(new Note(0, "A", "C", "user1", ""));
        repo.save(new Note(0, "B", "C", "user1", ""));
        repo.save(new Note(0, "C", "C", "user2", ""));

        List<Note> notes = repo.findByCreatore("user1");
        assertEquals(2, notes.size());
    }

    @Test
    void testSearchByUser() {
        NoteRepository repo = new NoteRepository();

        repo.save(new Note(0, "Spesa", "Compra latte", "user1", ""));
        repo.save(new Note(0, "Lavoro", "Preparare meeting", "user1", ""));

        List<Note> result = repo.searchByUser("user1", "latte");
        assertEquals(1, result.size());
    }

    @Test
    void testDelete() {
        NoteRepository repo = new NoteRepository();

        Note n = new Note(0, "Titolo", "Contenuto", "user1", "");
        repo.save(n);
        repo.delete(n.getId());

        assertNull(repo.findById(n.getId()));
    }
}