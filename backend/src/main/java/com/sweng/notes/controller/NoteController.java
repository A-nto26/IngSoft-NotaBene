package com.sweng.notes.controller;

import com.sweng.notes.dto.CreateNoteRequest;
import com.sweng.notes.dto.NoteUpdateRequest;
import com.sweng.notes.dto.ShareNoteRequest;
import com.sweng.notes.model.*;
import com.sweng.notes.service.NoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Controller REST per la gestione delle note.
 * Regole aggiornate:
 * - Il permesso non è modificabile dopo la creazione
 * - L'autore può SOLO aggiungere utenti
 * - Gli utenti condivisi possono togliere solo sé stessi
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

    private static final Logger log = LoggerFactory.getLogger(NoteController.class);
    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    // ============================================================
    // Utility
    // ============================================================

    private String normalizeUser(String user) {
        return (user == null ? null : user.trim().toLowerCase());
    }

    private boolean isAutore(Note n, String user) {
        return n != null
                && user != null
                && n.getCreatore() != null
                && n.getCreatore().equalsIgnoreCase(user);
    }

    // ============================================================
    // LETTURA NOTE
    // ============================================================

    @GetMapping
    public ResponseEntity<List<Note>> getNotes(
            @RequestParam("user") String user,
            @RequestParam(value = "mie", defaultValue = "true") boolean mie,
            @RequestParam(value = "condivise", defaultValue = "true") boolean condivise) {

        String norm = normalizeUser(user);
        if (norm == null || norm.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("📥 GET /api/notes user={}, mie={}, condivise={}", norm, mie, condivise);

        Set<Note> result = new LinkedHashSet<>();

        if (mie)
            result.addAll(noteService.getNotesByCreator(norm));
        if (condivise)
            result.addAll(noteService.getSharedNotes(norm));

        return ResponseEntity.ok(List.copyOf(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Note> getNoteById(@PathVariable int id) {
        Note n = noteService.getNoteById(id);
        return (n == null)
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(n);
    }

    @GetMapping("/shared/{username}")
    public ResponseEntity<List<Note>> getShared(@PathVariable String username) {
        String norm = normalizeUser(username);
        if (norm == null)
            return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(noteService.getSharedNotes(norm));
    }

    @GetMapping("/visible/{username}")
    public ResponseEntity<List<Note>> getVisible(@PathVariable String username) {
        String norm = normalizeUser(username);
        if (norm == null)
            return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(noteService.getVisibleNotesForUser(norm));
    }

    // ============================================================
    // CREAZIONE — Sprint 4
    // ============================================================

    @PostMapping
    public ResponseEntity<String> createNote(@RequestBody CreateNoteRequest req) {

        // Validazioni base
        if (req.getTitolo() == null || req.getTitolo().isBlank() ||
            req.getContenuto() == null || req.getContenuto().isBlank() ||
            req.getCreatore() == null || req.getCreatore().isBlank()) {

            return ResponseEntity.badRequest().body("⚠️ Titolo, contenuto e creatore sono obbligatori.");
        }

        // Normalizzazione creatore
        req.setCreatore(normalizeUser(req.getCreatore()));

        // Delega al service (che gestisce tutto!)
        Note n = noteService.create(req);

        return ResponseEntity.ok("✅ Nota '" + n.getTitolo() + "' creata con successo.");
    }

    // ============================================================
    // MODIFICA (con controllo lock)
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

        // 2) LOCK
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

        // 2.bis) CONTROLLO VERSIONE (evita overwrite)
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
                // Creiamo un DTO fittizio, come richiesto dallo Sprint 4
                ShareNoteRequest shareReq = new ShareNoteRequest();
                shareReq.setUtentiCondivisi(new ArrayList<>(nuovi));

                // Chiamata corretta
                noteService.shareNote(id, shareReq, effectiveUser);
            }
        }

        // 6) RILASCIA SEMPRE IL LOCK DOPO UPDATE
        noteService.unlock(id, effectiveUser);
        return ResponseEntity.ok("✏️ Nota aggiornata con successo.");
    }

    // ============================================================
    // ELIMINAZIONE NOTA
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
    // RIPRISTINO VERSIONI
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
    // CONDIVISIONE
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

            noteService.shareNote(id, req, autoreNorm);
            return ResponseEntity.ok("🤝 Nota condivisa con " + condivisiNorm);
        }

    // ============================================================
    // USCITA DALLA CONDIVISIONE
    // ============================================================

    @DeleteMapping("/{id}/share/{utente}")
    public ResponseEntity<String> removeUserFromShare(
        @PathVariable int id,
        @PathVariable String utente,
        @RequestParam("user") String requester) {

            String normUtente = normalizeUser(utente);
            String normRequester = normalizeUser(requester);

            if (!normUtente.equals(normRequester)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("❌ Puoi rimuovere solo te stesso dalla condivisione.");
            }

            noteService.removeSelf(id, normUtente);
            return ResponseEntity.ok("👋 Sei stato rimosso dalla nota.");
        }

    // ============================================================
    // DUPLICA NOTA
    // ============================================================

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<Note> duplicateNote(
        @PathVariable int id,
        @RequestParam("user") String user) {

            String norm = normalizeUser(user);
            return ResponseEntity.ok(noteService.duplicate(id, norm));
        }

    // ============================================================
    // LOCKING
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
     * Ottiene info sul lock (solo se ancora valido).
     */
    @GetMapping("/{id}/lock")
    public ResponseEntity<?> getLockState(@PathVariable int id) {
        Map<String, Object> state = noteService.getLockState(id);
        return ResponseEntity.ok(state);
    }
}