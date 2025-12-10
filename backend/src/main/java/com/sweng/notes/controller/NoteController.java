package com.sweng.notes.controller;

import com.sweng.notes.dto.CreateNoteRequest;
import com.sweng.notes.dto.NoteUpdateRequest;
import com.sweng.notes.dto.NoteView;
import com.sweng.notes.dto.ShareNoteRequest;
import com.sweng.notes.model.*;
import com.sweng.notes.service.NoteService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/*
* MANTENUTI PERCè USATI DAI METODI COMMENTATI E MANTENUTI PER COERENZA
* import org.slf4j.Logger;
* import org.slf4j.LoggerFactory;
*/

/**
 * Controller REST per la gestione delle note.
 * Regole principali:
 * - Il permesso NON è modificabile dopo la creazione
 * - L'autore può SOLO aggiungere utenti (MAI RIMUOVERLI)
 * - Gli utenti condivisi possono togliere solo sé stessi
 * - Lock concorrente con timeout e refresh
 */
@RestController
@RequestMapping("/api/notes")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
        RequestMethod.GET,
        RequestMethod.POST,
        RequestMethod.PUT,
        RequestMethod.DELETE,
        RequestMethod.OPTIONS
})
public class NoteController {

    // private static final Logger log =
    // LoggerFactory.getLogger(NoteController.class);
    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    // ============================================================
    // UTILITY INTERNE
    // ============================================================

    /** Normalizza username eliminando spazi ed evitando problemi di maiuscole */
    private String normalizeUser(String user) {
        return (user == null ? null : user.trim().toLowerCase());
    }

    /** Verifica se l’utente è autore della nota */
    private boolean isAutore(Note n, String user) {
        return n != null
                && user != null
                && n.getCreatore() != null
                && n.getCreatore().equalsIgnoreCase(user);
    }

    /*
     * // ============================================================
     * // METODI DI LETTURA NON USATI
     * // MANTENIAMO PER COMPLETEZZA
     * // ============================================================
     * 
     * @GetMapping
     * public ResponseEntity<List<Note>> getNotes(
     * 
     * @RequestParam("user") String user,
     * 
     * @RequestParam(value = "mie", defaultValue = "true") boolean mie,
     * 
     * @RequestParam(value = "condivise", defaultValue = "true") boolean condivise)
     * {
     * 
     * String norm = normalizeUser(user);
     * if (norm == null || norm.isBlank()) {
     * return ResponseEntity.badRequest().build();
     * }
     * 
     * log.info("📥 GET /api/notes user={}, mie={}, condivise={}", norm, mie,
     * condivise);
     * 
     * Set<Note> result = new LinkedHashSet<>();
     * 
     * if (mie)
     * result.addAll(noteService.getNotesByCreator(norm));
     * if (condivise)
     * result.addAll(noteService.getSharedNotes(norm));
     * 
     * return ResponseEntity.ok(List.copyOf(result));
     * }
     * 
     * @GetMapping("/{id}")
     * public ResponseEntity<Note> getNoteById(@PathVariable int id) {
     * Note n = noteService.getNoteById(id);
     * return (n == null)
     * ? ResponseEntity.notFound().build()
     * : ResponseEntity.ok(n);
     * }
     */

    @GetMapping("/{id}")
    public ResponseEntity<NoteView> getNoteById(
            @PathVariable int id,
            @RequestParam("user") String user) {

        String norm = normalizeUser(user);

        Note n = noteService.getNoteById(id);
        if (n == null)
            return ResponseEntity.notFound().build();

        NoteView v = noteService.toView(n, norm);
        return ResponseEntity.ok(v);
    }

    // ============================================================
    // GET - NOTE VISIBILI
    // ============================================================

    @GetMapping("/visible/{username}")
    public ResponseEntity<List<NoteView>> getVisible(@PathVariable String username) {

        String norm = normalizeUser(username);
        if (norm == null)
            return ResponseEntity.badRequest().build();

        List<Note> rawNotes = noteService.getVisibleNotesForUser(norm);

        // Converte ogni nota in NoteView filtrata
        List<NoteView> views = rawNotes.stream()
                .map(n -> noteService.toView(n, norm))
                .toList();

        return ResponseEntity.ok(views);
    }

    // ============================================================
    // POST - CREAZIONE
    // ============================================================

    @PostMapping
    public ResponseEntity<String> createNote(@RequestBody CreateNoteRequest req) {

        if (req.getTitolo() == null || req.getTitolo().isBlank() ||
                req.getContenuto() == null || req.getContenuto().isBlank() ||
                req.getCreatore() == null || req.getCreatore().isBlank()) {

            return ResponseEntity.badRequest().body("⚠️ Titolo, contenuto e creatore sono obbligatori.");
        }

        req.setCreatore(normalizeUser(req.getCreatore()));

        Note n = noteService.create(req);
        return ResponseEntity.ok("✅ Nota '" + n.getTitolo() + "' creata con successo.");
    }

    // ============================================================
    // PUT - MODIFICA (con controllo lock)
    // ============================================================
    @PutMapping("/{id}")
    public ResponseEntity<String> updateNote(
            @PathVariable int id,
            @RequestParam("user") String user,
            @RequestBody NoteUpdateRequest req) {

        Note n = noteService.getNoteById(id);
        if (n == null)
            return ResponseEntity.notFound().build();

        String effectiveUser = normalizeUser(user);
        if (effectiveUser == null) {
            return ResponseEntity.badRequest().body("⚠️ Utente non specificato.");
        }

        // 1) PERMESSO
        if (!n.puoScrivere(effectiveUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("❌ Non hai i permessi per modificare questa nota.");
        }

        // 2) LOCK (nuova logica)
        Optional<String> lockOwnerOpt = noteService.getLockOwner(id);

        if (lockOwnerOpt.isEmpty()) {
            // lock assente → scaduto oppure mai acquisito
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("⌛ Il lock è scaduto. Riapri la nota per continuare.");
        }

        String lockOwner = lockOwnerOpt.get();

        if (!lockOwner.equalsIgnoreCase(effectiveUser)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("❌ La nota è attualmente in modifica da: " + lockOwner);
        }

        // 2.5) CONTROLLO VERSIONE (evita overwrite)
        int versioneCorrente = (n.getVersioni() == null ? 1 : n.getVersioni().size() + 1);
        Integer versioneAttesa = req.getVersionExpected();

        if (versioneAttesa != null && !versioneAttesa.equals(versioneCorrente)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("❌ La nota è stata aggiornata da un altro utente. Ricarica la nota per continuare.");
        }

        // 3) UPDATE
        try {
            noteService.update(id, req, effectiveUser);
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("❌ " + ise.getMessage());
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("❌ " + se.getMessage());
        }

        // 4) CARTELLA (solo se cambiata)
        if (req.getCartella() != null &&
                !Objects.equals(n.getCartella(), req.getCartella())) {

            n.setCartella(req.getCartella().isBlank() ? null : req.getCartella());
            noteService.save(n);
        }

        // 5) AGGIUNTA UTENTI (solo autore)
        if (isAutore(n, effectiveUser) && req.getUtentiCondivisi() != null) {

            Set<String> nuovi = new HashSet<>();

            for (String u : req.getUtentiCondivisi()) {
                if (u != null && !u.isBlank()) {
                    String norm = normalizeUser(u);

                    if (!n.getUtentiCondivisi().contains(norm)) {
                        nuovi.add(norm);
                    }
                }
            }

            if (!nuovi.isEmpty()) {
                noteService.addUsersToShare(id, nuovi);
            }
        }

        // 6) RILASCIA LOCK DOPO UPDATE
        noteService.unlock(id, effectiveUser);
        return ResponseEntity.ok("✏️ Nota aggiornata con successo.");
    }

    // ============================================================
    // DELETE - ELIMINAZIONE NOTA
    // ============================================================

    @DeleteMapping("/{id}")
    @CrossOrigin(origins = "*")
    public ResponseEntity<String> deleteNote(
            @PathVariable int id,
            @RequestParam("user") String user) {

        String norm = normalizeUser(user);
        Note n = noteService.getNoteById(id);

        if (n == null)
            return ResponseEntity.notFound().build();

        // Solo autore può eliminare
        if (!n.getCreatore().equalsIgnoreCase(norm)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("❌ Solo l'autore può eliminare questa nota.");
        }

        noteService.delete(id, norm);

        return ResponseEntity.ok("🗑️ Nota eliminata con successo.");
    }

    // ============================================================
    // PUT - RIPRISTINO VERSIONI
    // ============================================================
    @PutMapping("/{id}/restore/{index}")
    public ResponseEntity<String> restoreVersion(
            @PathVariable int id,
            @PathVariable int index,
            @RequestParam("user") String user) {

        String norm = normalizeUser(user);
        Note n = noteService.getNoteById(id);

        if (n == null)
            return ResponseEntity.notFound().build();

        // 1) Controllo permessi
        if (!isAutore(n, norm) && !n.puoScrivere(norm)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("❌ Non hai i permessi per ripristinare una versione.");
        }

        // 2) Controllo indice
        if (index < 0 || index >= n.getVersioni().size()) {
            return ResponseEntity.badRequest().body("⚠️ Indice versione non valido.");
        }

        // 3) Controllo lock
        Optional<String> lockOwnerOpt = noteService.getLockOwner(id);

        if (lockOwnerOpt.isPresent() && !lockOwnerOpt.get().equalsIgnoreCase(norm)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("❌ La nota è attualmente in modifica da: " + lockOwnerOpt.get());
        }

        // 4) Effettua il restore
        try {
            noteService.restoreVersion(id, index, norm);
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("❌ " + ise.getMessage());
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("❌ " + se.getMessage());
        }

        return ResponseEntity.ok("🔙 Versione ripristinata.");
    }

    // ============================================================
    // POST - CONDIVISIONE
    // ============================================================

    @PostMapping("/{id}/share")
    public ResponseEntity<String> share(
            @PathVariable int id,
            @RequestBody ShareNoteRequest req,
            @RequestParam("user") String autore) {
        String autoreNorm = normalizeUser(autore);

        Note n = noteService.getNoteById(id);
        if (n == null)
            return ResponseEntity.notFound().build();

        if (req.getUtentiCondivisi() == null || req.getUtentiCondivisi().isEmpty()) {
            return ResponseEntity.badRequest().body("⚠️ Nessun utente da condividere.");
        }

        Set<String> condivisiNorm = new HashSet<>();
        for (String u : req.getUtentiCondivisi()) {
            if (u != null && !u.isBlank())
                condivisiNorm.add(u.trim().toLowerCase());
        }

        req.setUtentiCondivisi(new ArrayList<>(condivisiNorm));
        noteService.shareNote(id, req, autoreNorm);
        return ResponseEntity.ok("🤝 Nota condivisa con " + condivisiNorm);
    }

    // ============================================================
    // USCITA DALLA CONDIVISIONE
    // ============================================================

    @PostMapping("/{id}/removeSelf")
    public ResponseEntity<String> removeSelf(
            @PathVariable int id,
            @RequestBody Map<String, String> body) {

        String user = normalizeUser(body.get("user"));
        if (user == null || user.isBlank()) {
            return ResponseEntity.badRequest().body("⚠️ Utente mancante.");
        }

        Note n = noteService.getNoteById(id);
        if (n == null) {
            return ResponseEntity.notFound().build();
        }

        // L’utente può rimuovere SOLO sé stesso
        if (!n.getUtentiCondivisi().contains(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("❌ Non puoi rimuovere un altro utente.");
        }

        noteService.removeSelf(id, user);
        return ResponseEntity.ok("👋 Sei stato rimosso dalla nota.");
    }
    // ============================================================
    // POST - DUPLICA NOTA
    // ============================================================

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<Note> duplicateNote(
            @PathVariable int id,
            @RequestParam("user") String user) {

        String norm = normalizeUser(user);
        return ResponseEntity.ok(noteService.duplicate(id, norm));
    }

    // ============================================================
    // POST - LOCKING
    // ============================================================

    /**
     * Prova ad acquisire il lock.
     * Ritorna:
     * - 200 OK → lock acquisito
     * - 409 CONFLICT → lock detenuto da altro utente
     */
    @PostMapping("/{id}/lock")
    public ResponseEntity<?> lockNote(
            @PathVariable int id,
            @RequestParam("user") String user) {

        String norm = normalizeUser(user);
        if (norm == null)
            return ResponseEntity.badRequest().body("Utente mancante");

        String result = noteService.lock(id, norm);

        return switch (result) {
            case "locked" -> ResponseEntity.ok(Map.of("status", "locked"));
            case "expired_recovered" ->
                ResponseEntity.ok(Map.of("status", "expired_recovered"));
            case "already_locked" -> {
                Optional<String> owner = noteService.getLockOwner(id);
                yield ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("status", "already_locked",
                                "lockedBy", owner.orElse("sconosciuto")));
            }
            case "not_found" -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Nota inesistente");
            default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Errore lock");
        };
    }

    /**
     * Rinnova il lock se posseduto.
     */
    @PostMapping("/{id}/lock/refresh")
    public ResponseEntity<?> refreshLock(
            @PathVariable int id,
            @RequestParam("user") String user) {

        String norm = normalizeUser(user);
        if (norm == null)
            return ResponseEntity.badRequest().body("Utente mancante");

        noteService.refreshLockState(id, norm);
        return ResponseEntity.ok(Map.of("status", "refreshed"));
    }

    /**
     * Prova lo sblocco (volontario o scaduto).
     */
    @PostMapping("/{id}/unlock")
    public ResponseEntity<?> unlockNote(
            @PathVariable int id,
            @RequestParam("user") String user) {

        String norm = normalizeUser(user);
        if (norm == null)
            return ResponseEntity.badRequest().body("Utente mancante");

        String esito = noteService.unlock(id, norm);

        return switch (esito) {
            case "unlocked" -> ResponseEntity.ok(Map.of("status", "unlocked"));
            case "forbidden" -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("status", "forbidden"));
            default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error"));
        };
    }

    /**
     * GET - Ottiene info sul lock (solo se ancora valido).
     */
    @GetMapping("/{id}/lock")
    public ResponseEntity<?> getLockState(@PathVariable int id) {
        Map<String, Object> state = noteService.getLockState(id);
        return ResponseEntity.ok(state);
    }
}