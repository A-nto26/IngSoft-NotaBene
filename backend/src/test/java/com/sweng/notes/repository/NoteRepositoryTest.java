package com.sweng.notes.repository;

import com.sweng.notes.model.Note;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NoteRepositoryTest {

    @Test
    void testSaveAssignsIdIncrementally() {
        NoteRepository repo = new NoteRepository();

        Note n1 = repo.save("t1", "c1", "mario");
        Note n2 = repo.save("t2", "c2", "anna");

        assertEquals(1, n1.getId());
        assertEquals(2, n2.getId());
    }

    @Test
    void testFindByCreatore() {
        NoteRepository repo = new NoteRepository();

        repo.save("A", "B", "mario");
        repo.save("C", "D", "anna");
        repo.save("E", "F", "mario");

        List<Note> result = repo.findByCreatore("mario");

        assertEquals(2, result.size());
        for (Note n : result) {
            assertEquals("mario", n.getCreatore());
        }
    }
}
