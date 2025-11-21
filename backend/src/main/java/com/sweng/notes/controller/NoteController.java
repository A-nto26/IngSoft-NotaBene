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

    // ============================
    // CREATE
    // ============================
    @PostMapping
    public ResponseEntity<Note> create(
            @RequestParam String titolo,
            @RequestParam String contenuto,
            @RequestParam String creatore,
            @RequestParam(required = false) String cartella) {

        Note n = noteService.create(titolo, contenuto, creatore, cartella);
        return ResponseEntity.ok(n);
    }

    // ============================
    // GET NOTES BY USER
    // ============================
    @GetMapping("/{username}")
    public ResponseEntity<List<Note>> getByUser(@PathVariable String username) {
        return ResponseEntity.ok(noteService.getByUser(username));
    }

    // ============================
    // UPDATE (titolo + contenuto)
    // ============================
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(
            @PathVariable int id,
            @RequestParam(required = false) String titolo,
            @RequestParam(required = false) String contenuto) {

        noteService.update(id, titolo, contenuto);
        return ResponseEntity.ok().build();
    }

    // ============================
    // DUPLICATE
    // ============================
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<Note> duplicate(
            @PathVariable int id,
            @RequestParam String creatore) {

        Note duplicata = noteService.duplicate(id, creatore);
        return ResponseEntity.ok(duplicata);
    }

    // ============================
    // DELETE
    // ============================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        noteService.delete(id);
        return ResponseEntity.ok().build();
    }

    // ============================
    // SEARCH
    // ============================
    @GetMapping("/search")
    public ResponseEntity<List<Note>> search(
            @RequestParam String user,
            @RequestParam(required = false) String q) {

        return ResponseEntity.ok(noteService.search(user, q));
    }

    // ============================
    // ASSIGN FOLDER
    // ============================
    @PutMapping("/{id}/folder")
    public ResponseEntity<Void> setFolder(
            @PathVariable int id,
            @RequestParam String cartella) {

        noteService.setCartella(id, cartella);
        return ResponseEntity.ok().build();
    }
}