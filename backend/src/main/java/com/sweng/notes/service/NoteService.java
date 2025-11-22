package com.sweng.notes.service;

import com.sweng.notes.model.Note;
import com.sweng.notes.repository.NoteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NoteService {

    private final NoteRepository noteRepo;

    public NoteService(NoteRepository noteRepo) {
        this.noteRepo = noteRepo;
    }

    // ============================
    // CREATE
    // ============================
    public Note create(String titolo, String contenuto, String creatore, String cartella) {
        Note n = new Note(0, titolo, contenuto, creatore, cartella);
        noteRepo.save(n);
        return n;
    }

    // ============================
    // UPDATE (solo titolo e contenuto)
    // ============================
    public void update(int id, String nuovoTitolo, String nuovoContenuto) {
        Note note = noteRepo.findById(id);
        if (note == null)
            return;

        if (nuovoTitolo != null && !nuovoTitolo.isBlank()) {
            note.setTitolo(nuovoTitolo);
        }
        if (nuovoContenuto != null && !nuovoContenuto.isBlank()) {
            note.setContenuto(nuovoContenuto);
        }

        note.setLastModifiedAt(LocalDateTime.now());
        noteRepo.save(note);
    }

    // ============================
    // DELETE
    // ============================
    public void delete(int id) {
        noteRepo.delete(id);
    }

    // ============================
    // DUPLICATE
    // ============================
    public Note duplicate(int id, String creatore) {
        Note original = noteRepo.findById(id);
        if (original == null)
            return null;

        Note copy = new Note(
                0,
                original.getTitolo() + " (Copia)",
                original.getContenuto(),
                creatore,
                original.getCartella());

        noteRepo.save(copy);
        return copy;
    }

    // ============================
    // SEARCH
    // ============================
    public List<Note> search(String username, String query) {
        List<Note> userNotes = noteRepo.findByCreatore(username);
        if (query == null || query.isBlank())
            return userNotes;

        String q = query.toLowerCase();
        return userNotes.stream()
                .filter(n -> n.getTitolo().toLowerCase().contains(q) ||
                        n.getContenuto().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    // ============================
    // CARTELLA
    // ============================
    public void setCartella(int id, String cartella) {
        Note note = noteRepo.findById(id);
        if (note == null)
            return;

        note.setCartella(cartella);
        noteRepo.save(note);
    }

    // ============================
    // LISTA NOTE
    // ============================
    public List<Note> getByUser(String username) {
        return noteRepo.findByCreatore(username);
    }

    public Note getById(int id) {
        return noteRepo.findById(id);
    }
}