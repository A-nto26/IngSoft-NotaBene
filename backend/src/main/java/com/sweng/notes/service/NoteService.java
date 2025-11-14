package com.sweng.notes.service;

import com.sweng.notes.model.Note;
import com.sweng.notes.repository.NoteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoteService {

    private final NoteRepository noteRepo;

    public NoteService(NoteRepository noteRepo) {
        this.noteRepo = noteRepo;
    }

    public Note create(String titolo, String contenuto, String creatore) {
        return noteRepo.save(titolo, contenuto, creatore);
    }

    public List<Note> getByUser(String username) {
        return noteRepo.findByCreatore(username);
    }
}
