package com.sweng.notes.controller;

import com.sweng.notes.dto.FolderRequest;
import com.sweng.notes.model.Cartella;
import com.sweng.notes.model.Note;
import com.sweng.notes.service.FolderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST per la gestione delle cartelle persistenti.
 * Permette di creare, leggere, eliminare e visualizzare note per cartella.
 */
@RestController
@RequestMapping("/api/folders")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
        RequestMethod.GET,
        RequestMethod.POST,
        RequestMethod.PUT,
        RequestMethod.DELETE,
        RequestMethod.OPTIONS
})
public class FolderController {

    private final FolderService folderService;

    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    // ============================================================
    // LETTURA
    // ============================================================

    /** Restituisce tutte le cartelle persistenti */
    @GetMapping
    public ResponseEntity<List<Cartella>> getAllFolders() {
        List<Cartella> folders = folderService.getAllFolders();
        return ResponseEntity.ok(folders);
    }

    /** Restituisce tutte le note contenute in una cartella */
    @GetMapping("/{nome}")
    public ResponseEntity<List<Note>> getNotesInFolder(@PathVariable String nome) {
        List<Note> notes = folderService.getNotesInFolder(nome);
        return ResponseEntity.ok(notes);
    }

    // ============================================================
    // CREAZIONE
    // ============================================================

    /** Crea una nuova cartella persistente con colore e autore */
    @PostMapping
    public ResponseEntity<String> createFolder(@RequestBody FolderRequest req) {
        if (req.getNome() == null || req.getNome().isBlank()) {
            return ResponseEntity.badRequest().body("⚠️ Il nome della cartella è obbligatorio.");
        }

        String colore = (req.getColore() == null || req.getColore().isBlank())
                ? "#FFD700"
                : req.getColore();

        String creatore = (req.getCreatore() == null || req.getCreatore().isBlank())
                ? "system"
                : req.getCreatore();

        try {
            folderService.createFolder(req.getNome().trim().toLowerCase(), colore, creatore);
            return ResponseEntity.ok("📁 Cartella '" + req.getNome() + "' creata con colore " + colore);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    // ============================================================
    // ELIMINAZIONE
    // ============================================================

    /** Elimina una cartella esistente */
    @DeleteMapping("/{nome}")
    public ResponseEntity<String> deleteFolder(@PathVariable String nome) {
        folderService.deleteFolder(nome);
        return ResponseEntity.ok("🗑️ Cartella '" + nome + "' eliminata con successo.");
    }

    // ============================================================
    // NOTE VISIBILI PER UTENTE
    // ============================================================

    /** Restituisce tutte le note (proprie o condivise) visibili per un utente */
    @GetMapping("/{nome}/user/{username}")
    public ResponseEntity<List<Note>> getNotesInFolderForUser(
            @PathVariable String nome,
            @PathVariable String username) {

        if (nome == null || nome.isBlank() || username == null || username.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        List<Note> notes = folderService.getNotesInFolderForUser(nome, username);
        return ResponseEntity.ok(notes);
    }
}