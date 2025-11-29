package com.sweng.notes.controller;

import com.sweng.notes.dto.*;
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

    // ============================================================
    // CREATE — UC4 (Sprint 3)
    // ============================================================
    @PostMapping
    public ResponseEntity<Note> create(@RequestBody CreateNoteRequest req) {
        return ResponseEntity.ok(noteService.create(req));
    }

    // ============================================================
    // VISUALIZZA NOTE VISIBILI — UC3
    // ============================================================
    @GetMapping("/visibili/{username}")
    public ResponseEntity<List<Note>> getVisibleNotes(@PathVariable String username) {
        String userNorm = username.trim().toLowerCase();
        return ResponseEntity.ok(noteService.getVisibleNotes(userNorm));
    }

    // ============================================================
    // UPDATE — UC10 (versionamento + controllo autore)
    // ============================================================
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(
            @PathVariable int id,
            @RequestParam String user,
            @RequestBody NoteUpdateRequest req) {

        String userNorm = user.trim().toLowerCase();
        noteService.update(id, req, userNorm);
        return ResponseEntity.ok().build();
    }

    // ============================================================
    // DUPLICATE — UC6
    // ============================================================
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<Note> duplicate(
            @PathVariable int id,
            @RequestParam String user) {

        String userNorm = user.trim().toLowerCase();
        return ResponseEntity.ok(noteService.duplicate(id, userNorm));
    }

    // ============================================================
    // DELETE — UC12
    // ============================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable int id,
            @RequestParam String user) {

        String userNorm = user.trim().toLowerCase();
        noteService.delete(id, userNorm);
        return ResponseEntity.ok().build();
    }

    // ============================================================
    // SEARCH — UC8
    // ============================================================
    @GetMapping("/search")
    public ResponseEntity<List<Note>> search(
            @RequestParam String user,
            @RequestParam(required = false) String q) {

        String userNorm = user.trim().toLowerCase();
        return ResponseEntity.ok(noteService.search(userNorm, q));
    }

    // ============================================================
    // ASSEGNA CARTELLA — UC9
    // ============================================================
    @PutMapping("/{id}/folder")
    public ResponseEntity<Void> setFolder(
            @PathVariable int id,
            @RequestParam String user,
            @RequestBody FolderRequest req) {

        String userNorm = user.trim().toLowerCase();
        noteService.setCartella(id, req.getNome(), userNorm);
        return ResponseEntity.ok().build();
    }

    // ============================================================
    // SHARE — UC11
    // ============================================================
    @PostMapping("/{id}/share")
    public ResponseEntity<Void> share(
            @PathVariable int id,
            @RequestParam String user,
            @RequestBody ShareNoteRequest req) {

        String userNorm = user.trim().toLowerCase();
        noteService.shareNote(id, req, userNorm);
        return ResponseEntity.ok().build();
    }

    // ============================================================
    // RIMOZIONE DI SE STESSI — UC7
    // ============================================================
    @PostMapping("/{id}/removeSelf")
    public ResponseEntity<Void> removeSelf(
            @PathVariable int id,
            @RequestBody UserRequest req) {

        String userNorm = req.getUsername().trim().toLowerCase();
        noteService.removeSelf(id, userNorm);
        return ResponseEntity.ok().build();
    }

    // ============================================================
    // RESTORE VERSIONE — UC5
    // ============================================================
    @PostMapping("/{id}/restore/{index}")
    public ResponseEntity<Void> restoreVersion(
            @PathVariable int id,
            @PathVariable int index,
            @RequestParam String user) {

        String userNorm = user.trim().toLowerCase();
        noteService.restoreVersion(id, index, userNorm);
        return ResponseEntity.ok().build();
    }
}
