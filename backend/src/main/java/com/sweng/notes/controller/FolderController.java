package com.sweng.notes.controller;

import com.sweng.notes.dto.FolderRequest;
import com.sweng.notes.dto.FolderResponse;
import com.sweng.notes.model.Cartella;
import com.sweng.notes.model.Note;
import com.sweng.notes.service.FolderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST per la gestione delle cartelle.
 * Funzionalità:
 * - creazione cartella,
 * - lettura cartelle,
 * - eliminazione cartella
 * - visualizzare note all'interno di una cartella per utente.
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
    // GET - LETTURA CARTELLE
    // ============================================================

    /** Restituisce tutte le cartelle persistenti */
    @GetMapping
    public ResponseEntity<List<FolderResponse>> getAllFolders() {

        List<Cartella> folders = folderService.getAllFolders();

        List<FolderResponse> resp = folders.stream()
                .map(FolderResponse::fromCartella)
                .toList();

        return ResponseEntity.ok(resp);
    }

    /*
     * NON USATO MANTENUTO PER COMPLETEZZA - Restituisce tutte le note contenute in
     * una cartella
     * 
     * @GetMapping("/{nome}")
     * public ResponseEntity<List<Note>> getNotesInFolder(@PathVariable String nome)
     * {
     * List<Note> notes = folderService.getNotesInFolder(nome);
     * return ResponseEntity.ok(notes);
     * }
     */

    // ============================================================
    // POST - CREAZIONE CARTELLA
    // ============================================================

    /** Crea una nuova cartella persistente con colore e autore */
    @PostMapping
    public ResponseEntity<String> createFolder(@RequestBody FolderRequest req) {

        if (req.getNome() == null || req.getNome().isBlank()) {
            return ResponseEntity.badRequest().body("⚠️ Il nome della cartella è obbligatorio.");
        }

        String nome = req.getNome();
        String colore = req.getColore();
        String creatore = req.getCreatore();
        try {
            folderService.createFolder(nome, colore, creatore);
            return ResponseEntity.ok("📁 Cartella '" + nome + "' creata.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    // ============================================================
    // DELETE - ELIMINAZIONE CARTELLA
    // ============================================================

    /** Elimina una cartella esistente */
    @DeleteMapping("/{nome}")
    public ResponseEntity<String> deleteFolder(@PathVariable String nome) {
        folderService.deleteFolder(nome);
        return ResponseEntity.ok("🗑️ Cartella '" + nome + "' eliminata con successo.");
    }

    // ============================================================
    // GET - NOTE VISIBILI PER UTENTE
    // ============================================================

    /**
     * Restituisce tutte le note visibili (proprie o condivise)
     * per un utente all'interno di una cartella
     */
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