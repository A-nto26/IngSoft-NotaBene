package com.sweng.notes.controller;

import com.sweng.notes.model.Note;
import com.sweng.notes.service.NoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
@CrossOrigin(origins = "*")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping
    public ResponseEntity<Note> create(
            @RequestParam String titolo,
            @RequestParam String contenuto,
            @RequestParam String creatore) {
        return ResponseEntity.ok(noteService.create(titolo, contenuto, creatore));
    }

    @GetMapping("/{username}")
    public ResponseEntity<List<Note>> getByUser(@PathVariable String username) {
        return ResponseEntity.ok(noteService.getByUser(username));
    }
}
