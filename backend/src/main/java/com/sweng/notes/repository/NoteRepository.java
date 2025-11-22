package com.sweng.notes.repository;

import com.sweng.notes.model.Note;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class NoteRepository {

    private final Map<Integer, Note> notes = new LinkedHashMap<>();
    private int idCounter = 1;

    // ============================================
    // CREATE + UPDATE
    // ============================================

    public synchronized Note save(Note note) {
        if (note.getId() == 0) {
            note.setId(idCounter++);
        }
        notes.put(note.getId(), note);
        return note;
    }

    // ============================================
    // READ
    // ============================================

    public Note findById(int id) {
        return notes.get(id);
    }

    public List<Note> findByCreatore(String username) {
        List<Note> result = new ArrayList<>();
        for (Note n : notes.values()) {
            if (username.equalsIgnoreCase(n.getCreatore())) {
                result.add(n);
            }
        }
        return result;
    }

    // ============================================
    // DELETE
    // ============================================

    public void delete(int id) {
        notes.remove(id);
    }

    // ============================================
    // SEARCH
    // ============================================

    public List<Note> searchByUser(String username, String query) {
        List<Note> userNotes = findByCreatore(username);

        if (query == null || query.isBlank()) {
            return userNotes;
        }

        String q = query.toLowerCase();
        List<Note> result = new ArrayList<>();

        for (Note n : userNotes) {
            if (n.getTitolo().toLowerCase().contains(q) ||
                    n.getContenuto().toLowerCase().contains(q)) {
                result.add(n);
            }
        }

        return result;
    }
}