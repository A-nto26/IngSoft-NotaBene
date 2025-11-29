package com.sweng.notes.controller;

import com.sweng.notes.dto.FolderRequest;
import com.sweng.notes.model.Cartella;
import com.sweng.notes.model.Note;
import com.sweng.notes.service.FolderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/folders")
@CrossOrigin(origins = "*")
public class FolderController {

    private final FolderService folderService;

    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    // ============================================================
    // LETTURA CARTELLE
    // ============================================================

    @GetMapping
    public ResponseEntity<List<Cartella>> getAllFolders() {
        return ResponseEntity.ok(folderService.getAllFolders());
    }

    @GetMapping("/{nome}")
    public ResponseEntity<List<Note>> getNotesInFolder(@PathVariable String nome) {
        return ResponseEntity.ok(folderService.getNotesInFolder(nome));
    }

    // ============================================================
    // CREAZIONE CARTELLA
    // ============================================================

    @PostMapping
    public ResponseEntity<String> createFolder(@RequestBody FolderRequest req) {

        if (req.getNome() == null || req.getNome().isBlank()) {
            return ResponseEntity.badRequest().body("Il nome della cartella è obbligatorio.");
        }

        String nome = req.getNome().trim();

        String colore = (req.getColore() == null || req.getColore().isBlank())
                ? "#FFD700"
                : req.getColore();

        String creatore = (req.getCreatore() == null || req.getCreatore().isBlank())
                ? "system"
                : req.getCreatore();

        try {
            folderService.createFolder(nome, colore, creatore);
            return ResponseEntity.ok("Cartella '" + nome + "' creata correttamente.");
        } catch (Exception e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    // ============================================================
    // ELIMINAZIONE CARTELLA
    // ============================================================

    @DeleteMapping("/{nome}")
    public ResponseEntity<String> deleteFolder(@PathVariable String nome) {

        if (nome == null || nome.isBlank()) {
            return ResponseEntity.badRequest().body("Nome cartella non valido.");
        }

        folderService.deleteFolder(nome);
        return ResponseEntity.ok("Cartella '" + nome + "' eliminata.");
    }

    // ============================================================
    // NOTE VISIBILI PER UTENTE
    // ============================================================

    @GetMapping("/{nome}/user/{username}")
    public ResponseEntity<List<Note>> getNotesInFolderForUser(
            @PathVariable String nome,
            @PathVariable String username) {

        return ResponseEntity.ok(folderService.getNotesInFolderForUser(nome, username));
    }
}
