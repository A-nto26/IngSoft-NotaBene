package com.sweng.notes.repository;

import com.sweng.notes.model.Note;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class NoteRepository {

    private final Map<Integer, Note> notes = new HashMap<>();
    private int idCounter = 1;

    public Note save(String titolo, String contenuto, String creatore) {
        Note n = new Note(idCounter++, titolo, contenuto, creatore);
        notes.put(n.getId(), n);
        return n;
    }

    public List<Note> findByCreatore(String username) {
        List<Note> result = new ArrayList<>();
        for (Note n : notes.values()) {
            if (n.getCreatore().equals(username)) {
                result.add(n);
            }
        }
        return result;
    }
}
