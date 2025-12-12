package com.sweng.notes.service;

import com.sweng.notes.dto.*;
import com.sweng.notes.logging.LoggerActions;
import com.sweng.notes.model.*;
import com.sweng.notes.repository.NoteRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Servizio principale per la gestione delle note.
 * 
 * Funzionalità:
 * - Creazione con permesso iniziale (non modificabile successivamente)
 * - Visibilità (note proprie + condivise)
 * - Versionamento (salvataggio automatico + restore)
 * - Cartelle e colore associato
 * - Condivisione con regole aggiornate (autore aggiunge; utenti rimossi solo da
 * sé stessi)
 * - Lock concorrente completo (acquisizione, refresh, timeout, unlock)
 */
@Service
public class NoteService {

    private final NoteRepository repo;

    public NoteService(NoteRepository repo) {
        this.repo = repo;
    }

    // ============================================================
    // UTILITÀ
    // ============================================================

    private String normalize(String s) {
        return (s == null) ? null : s.trim().toLowerCase();
    }

    // ============================================================
    // CONVERSIONE NOTE, ATTRAVERSO NOTEVIEW LEGGIAMO LE INFO DELLA NOTA PER IL
    // FRONTEND
    // ============================================================

    public NoteView toView(Note note, String currentUser) {
        if (note == null)
            return null;

        String normUser = normalize(currentUser);

        NoteView v = new NoteView();

        // ============================================================
        // CAMPI BASE
        // ============================================================
        v.setId(note.getId());
        v.setTitolo(note.getTitolo());
        v.setContenuto(note.getContenuto());
        v.setCartella(note.getCartella());
        v.setColoreCartella(note.getColoreCartella());
        v.setCreatore(note.getCreatore());

        // ============================================================
        // PERMESSO
        // ============================================================
        String perm = "privata";
        if (note.getPermesso() != null) {
            perm = note.getPermesso().getTipo().toLowerCase();
        }
        v.setPermesso(perm);

        // ============================================================
        // RUOLO UTENTE (autore | scrittura | lettura | hidden)
        // ============================================================
        String ruolo;
        if (note.getCreatore().equalsIgnoreCase(normUser)) {
            ruolo = "autore";
        } else if (note.puoScrivere(normUser)) {
            ruolo = "scrittura";
        } else if (note.puoLeggere(normUser)) {
            ruolo = "lettura";
        } else {
            ruolo = "hidden";
        }
        v.setRuolo(ruolo);

        // ============================================================
        // LOCK OWNER
        // ============================================================
        String lockedBy = repo.getEffectiveLockOwner(note.getId()).orElse(null);
        v.setLockedBy(lockedBy);

        // ============================================================
        // VERSIONE CORRENTE
        // ============================================================
        int versioneCorrente = (note.getVersioni() == null)
                ? 1
                : note.getVersioni().size() + 1;
        v.setVersione(versioneCorrente);

        // ============================================================
        // LIMIT VERSIONI RAGGIUNTO
        // ============================================================
        boolean limit = note.getVersioni() != null && note.getVersioni().size() >= 50;
        v.setVersionLimitReached(limit);

        // ============================================================
        // PERMESSI → canChangePermission
        // ============================================================
        boolean isAuthor = note.getCreatore().equalsIgnoreCase(normUser);

        boolean hasSharedUsers = note.getUtentiCondivisi() != null &&
                !note.getUtentiCondivisi().isEmpty();

        boolean canChange = isAuthor &&
                !(perm.equals("privata") && hasSharedUsers);

        v.setCanChangePermission(canChange);

        // ============================================================
        // TIMESTAMP
        // ============================================================
        v.setCreatedAt(note.getCreatedAt());
        v.setLastModifiedAt(note.getLastModifiedAt());
        v.setLastModifiedBy(note.getLastModifiedBy());

        // ============================================================
        // LISTA UTENTI CONDIVISI FILTRATA
        // ============================================================
        List<String> condivisi = new ArrayList<>();

        if (note.getUtentiCondivisi() != null) {
            for (String u : note.getUtentiCondivisi()) {

                if (u.equalsIgnoreCase(normUser))
                    continue; // escludi corrente
                if (u.equalsIgnoreCase(note.getCreatore()))
                    continue; // escludi autore

                condivisi.add(u);
            }
        }

        v.setCondivisaCon(condivisi);

        // ============================================================
        // VERSIONI
        // ============================================================
        v.setVersioni(
                note.getVersioni() != null
                        ? new ArrayList<>(note.getVersioni())
                        : List.of());

        return v;
    }

    // ============================================================
    // CREAZIONE
    // ============================================================
    public Note create(CreateNoteRequest req) {
        if (req == null || req.getCreatore() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dati nota non validi");
        }

        String creatoreNorm = normalize(req.getCreatore());
        if (creatoreNorm == null || creatoreNorm.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Creatore non valido");
        }

        // =============== CREAZIONE NOTA ===============
        Note n = new Note();
        n.setTitolo(req.getTitolo());
        n.setContenuto(req.getContenuto());
        n.setCreatore(creatoreNorm);
        n.setCartella(req.getCartella());

        // Timestamp iniziali
        n.setCreatedAt(LocalDateTime.now());
        n.setLastModifiedAt(LocalDateTime.now());
        n.setLastModifiedBy(creatoreNorm);

        // Nessun lock iniziale
        n.setLockedBy(null);
        n.setLockedAt(null);

        // Permesso: impostato SOLO in creazione
        Permesso p;
        if ("LETTURA".equalsIgnoreCase(req.getPermesso())) {
            p = new Lettura();
        } else if ("SCRITTURA".equalsIgnoreCase(req.getPermesso())) {
            p = new Scrittura();
        } else {
            p = new Privata();
        }
        n.setPermesso(p);

        // Utenti condivisi: dipende dal permesso
        if (p instanceof Privata) {
            n.setUtentiCondivisi(new LinkedHashSet<>());
        } else if (req.getUtentiCondivisi() != null) {
            n.setUtentiCondivisi(new LinkedHashSet<>(req.getUtentiCondivisi()));
        } else {
            n.setUtentiCondivisi(new LinkedHashSet<>());
        }

        // Versioni inizialmente vuote
        n.setVersioni(new ArrayList<>());

        // Colore cartella
        if (req.getColoreCartella() != null && !req.getColoreCartella().isBlank()) {
            n.setColoreCartella(req.getColoreCartella().trim());
        } else {
            n.setColoreCartella("#ffb347");
        }

        repo.save(n);
        LoggerActions.log("NOTE_CREATE_SUCCESS", creatoreNorm, Map.of(
                "noteId", n.getId()));
        return n;
    }

    // ============================================================
    // GET VISIBLE
    // ============================================================
    public List<Note> getVisibleNotesForUser(String username) {
        return getNotesForUserFiltered(username, true, true);
    }

    public List<Note> getNotesForUserFiltered(
            String username,
            boolean mie,
            boolean condivise) {

        String norm = normalize(username);
        if (norm == null || norm.isBlank()) {
            throw new IllegalArgumentException("Username non può essere nullo o vuoto");
        }

        Set<Note> result = new LinkedHashSet<>();

        if (mie)
            result.addAll(repo.findByCreator(norm));

        if (condivise)
            result.addAll(repo.findSharedWithUser(norm));

        return List.copyOf(result);
    }

    /* ===== METODI DI SUPPORTO PER TEST ===== */
    public List<Note> getNotesForUser(String username) {
        return getVisibleNotesForUser(username);
    }

    public List<Note> getAllNotes() {
        return repo.findAll();
    }

    public List<Note> getNotesByCartella(String cartella) {
        return repo.findByCartella(cartella);
    }

    // ============================================================
    // Restituisce una singola nota in base al suo ID.
    // Metodo utilizzato dal Controller per tutte le operazioni applicative
    // (update, delete, share, restore, duplicate, lock).
    // ============================================================

    public Note getNoteById(int id) {
        return repo.findById(id);
    }

    // ============================================================
    // UPDATE
    // ============================================================
    public Note update(int id, NoteUpdateRequest req, String username) {

        String userNorm = normalize(username);
        if (userNorm == null || userNorm.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username non valido");
        }

        Note n = repo.findById(id);
        if (n == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota non trovata");
        }

        // 1) Permessi
        if (!n.puoScrivere(userNorm)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non hai i permessi per modificare questa nota");
        }

        // 2) Lock: deve essere valido e posseduto dall'utente
        Optional<String> lockOwnerOpt = repo.getEffectiveLockOwner(id);

        if (lockOwnerOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lock scaduto. Riapri la nota per continuare.");
        }

        String lockOwner = lockOwnerOpt.get();
        if (!lockOwner.equalsIgnoreCase(userNorm)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La nota è in modifica da " + lockOwner + ".");
        }

        // ============================================================
        // 3) NORMALIZZAZIONE INPUT (evita versioni fantasma)
        // ============================================================

        String newTitolo = (req.getTitolo() == null || req.getTitolo().isBlank())
                ? n.getTitolo()
                : req.getTitolo();

        String newContenuto = (req.getContenuto() == null)
                ? n.getContenuto()
                : req.getContenuto();

        String newCartella = (req.getCartella() == null || req.getCartella().isBlank())
                ? n.getCartella()
                : req.getCartella();

        String newColore = (req.getColoreCartella() == null || req.getColoreCartella().isBlank())
                ? n.getColoreCartella()
                : req.getColoreCartella().trim();

        // ============================================================
        // 4) VERIFICA MODIFICHE REALI
        // ============================================================

        boolean titoloChanged = !Objects.equals(newTitolo, n.getTitolo());
        boolean contenutoChanged = !Objects.equals(newContenuto, n.getContenuto());
        boolean cartellaChanged = !Objects.equals(newCartella, n.getCartella());
        boolean coloreChanged = !Objects.equals(newColore, n.getColoreCartella());

        boolean modificheRilevanti = titoloChanged || contenutoChanged || cartellaChanged || coloreChanged;

        // ============================================================
        // 4B) PERMESSO — aggiornabile (Sprint 5)
        // ============================================================
        boolean permessoChanged = false;
        String nuovoPermesso = req.getPermesso();

        if (nuovoPermesso != null && !nuovoPermesso.isBlank()) {
            String permNorm = nuovoPermesso.trim().toLowerCase();
            String permAttuale = n.getPermesso().getTipo().toLowerCase();

            if (!permNorm.equals(permAttuale)) {
                permessoChanged = true;
            }
        }

        // ============================================================
        // 5) SALVATAGGIO VERSIONE PRECEDENTE (solo se necessario)
        // ============================================================

        if (modificheRilevanti) {
            boolean versioneSalvata = n.salvaVersionePrecedente();

            if (!versioneSalvata) {
                LoggerActions.log(
                        "NOTE_VERSION_LIMIT_REACHED",
                        userNorm,
                        Map.of("noteId", id));
            }
        }

        // ============================================================
        // 6) APPLICA LE MODIFICHE REALI
        // ============================================================

        if (titoloChanged) {
            n.setTitolo(newTitolo);
        }

        if (contenutoChanged) {
            n.setContenuto(newContenuto);
        }

        if (cartellaChanged) {
            n.setCartella(newCartella);
        }

        if (coloreChanged) {
            n.setColoreCartella(newColore);
        }

        n.setLastModifiedAt(LocalDateTime.now());
        n.setLastModifiedBy(userNorm);

        if (permessoChanged) {

            // Non puoi tornare privata se ci sono utenti condivisi
            if ("privata".equalsIgnoreCase(nuovoPermesso)
                    && n.getUtentiCondivisi() != null
                    && !n.getUtentiCondivisi().isEmpty()) {

                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Non puoi impostare 'Privata' mentre la nota è condivisa.");
            }

            // Converte la stringa nel modello Permesso
            Permesso p;
            switch (nuovoPermesso.toLowerCase()) {
                case "lettura" -> p = new Lettura();
                case "scrittura" -> p = new Scrittura();
                default -> p = new Privata();
            }

            n.setPermesso(p);
        }
        repo.save(n);

        LoggerActions.log("NOTE_UPDATE_SUCCESS", userNorm, Map.of("noteId", id));

        return n;
    }

    /* ===== METODO AUSILIARE PER TEST ===== */
    public void save(Note note) {
        if (note != null)
            repo.save(note);
    }

    // ============================================================
    // DELETE
    // ============================================================
    public void delete(int id, String username) {

        String userNorm = normalize(username);
        if (userNorm == null || userNorm.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username non valido");
        }

        Note n = repo.findById(id);
        if (n == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota non trovata");
        }

        if (!Objects.equals(n.getCreatore(), userNorm)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato");
        }

        repo.delete(id);
        LoggerActions.log("NOTE_DELETE_SUCCESS", userNorm, Map.of("noteId", id));

    }

    // ============================================================
    // DUPLICAZIONE
    // ============================================================
    public Note duplicate(int id, String username) {

        String userNorm = normalize(username);
        if (userNorm == null || userNorm.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username non valido");
        }

        Note orig = repo.findById(id);
        if (orig == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota non trovata");
        }

        if (!orig.puoLeggere(userNorm)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato");
        }

        Note copia = new Note(
                0,
                orig.getTitolo() + " (Copia)",
                orig.getContenuto(),
                userNorm,
                orig.getCartella());

        // La copia eredita titolo, contenuto e cartella, ma:
        // - diventa PRIVATA
        // - non eredita utenti condivisi
        // - non eredita versioni precedenti
        copia.setPermesso(new Privata());
        copia.setVersioni(new ArrayList<>());
        copia.setUtentiCondivisi(new LinkedHashSet<>());
        copia.setColoreCartella(orig.getColoreCartella());
        copia.setCreatedAt(LocalDateTime.now());
        copia.setLastModifiedAt(LocalDateTime.now());
        copia.setLastModifiedBy(userNorm);

        repo.save(copia);
        LoggerActions.log("NOTE_DUPLICATE_SUCCESS", userNorm, Map.of(
                "originalId", id,
                "newId", copia.getId()));
        return copia;
    }

    // ============================================================
    // REMOVE SELF
    // ============================================================
    public void removeSelf(int id, String username) {

        String userNorm = normalize(username);
        if (userNorm == null || userNorm.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username non valido");
        }

        Note n = repo.findById(id);
        if (n == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota non trovata");
        }

        if (n.getUtentiCondivisi() == null || !n.getUtentiCondivisi().contains(userNorm)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nota non condivisa con questo utente");
        }

        repo.removeSelf(id, userNorm);
        LoggerActions.log("NOTE_UNSHARE_SELF", userNorm, Map.of("noteId", id));
    }

    // ============================================================
    // RICERCA
    // ============================================================
    public List<Note> search(String username, String query) {

        String userNorm = normalize(username);
        if (userNorm == null || userNorm.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username non valido");
        }

        List<Note> visibili = getVisibleNotesForUser(userNorm);

        if (query == null || query.isBlank()) {
            return visibili;
        }

        String q = query.toLowerCase();
        List<Note> result = new ArrayList<>();

        for (Note n : visibili) {
            if ((n.getTitolo() != null && n.getTitolo().toLowerCase().contains(q))
                    || (n.getContenuto() != null && n.getContenuto().toLowerCase().contains(q))) {
                result.add(n);
            }
        }

        return result;
    }

    // ============================================================
    // SET CARTELLA
    // ============================================================
    public void setCartella(int id, String nuovoNome, String username) {

        String userNorm = normalize(username);
        if (userNorm == null || userNorm.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username non valido");
        }

        Note n = repo.findById(id);
        if (n == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota non trovata");
        }

        if (!Objects.equals(n.getCreatore(), userNorm)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato");
        }

        n.salvaVersionePrecedente();
        n.setCartella(nuovoNome);
        n.setLastModifiedAt(LocalDateTime.now());
        n.setLastModifiedBy(userNorm);

        repo.save(n);
        LoggerActions.log("NOTE_FOLDER_CHANGED", userNorm, Map.of(
                "noteId", id,
                "folder", nuovoNome));

    }

    // ============================================================
    // SHARE
    // ============================================================
    public void shareNote(int id, ShareNoteRequest req, String autore) {

        String autoreNorm = normalize(autore);
        if (autoreNorm == null || autoreNorm.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Autore non valido");
        }

        Note n = repo.findById(id);
        if (n == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota non trovata");
        }

        if (!Objects.equals(n.getCreatore(), autoreNorm)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato");
        }

        if (req.getUtentiCondivisi() == null || req.getUtentiCondivisi().isEmpty()) {
            return;
        }

        repo.addUsersToShare(id, new LinkedHashSet<>(req.getUtentiCondivisi()));
        LoggerActions.log("NOTE_SHARE_ADD_USERS", autoreNorm, Map.of(
                "noteId", id,
                "usersAdded", req.getUtentiCondivisi()));
    }

    /* ===== METODO AUSILIARE PER TEST ===== */
    public void addUsersToShare(int id, Set<String> nuovi) {
        if (nuovi == null || nuovi.isEmpty())
            return;
        repo.addUsersToShare(id, nuovi);
    }

    /* ===== METODO AUSILIARE PER TEST ===== */
    public List<Note> getSharedNotes(String username) {
        String norm = normalize(username);
        if (norm == null)
            return List.of();
        return repo.findSharedWithUser(norm);
    }

    /* ===== METODO AUSILIARE PER TEST ===== */
    public List<Note> getNotesByCreator(String username) {
        String norm = normalize(username);
        if (norm == null)
            return List.of();
        return repo.findByCreator(norm);
    }

    // ============================================================
    // RESTORE VERSION
    // ============================================================
    public void restoreVersion(int id, int index, String username) {

        String userNorm = normalize(username);
        if (userNorm == null || userNorm.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username non valido");
        }

        Note n = repo.findById(id);
        if (n == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota non trovata");
        }

        if (!Objects.equals(n.getCreatore(), userNorm) && !n.puoScrivere(userNorm)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato");
        }

        Optional<String> lockOwnerOpt = repo.getEffectiveLockOwner(id);
        if (lockOwnerOpt.isPresent() && !lockOwnerOpt.get().equalsIgnoreCase(userNorm)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La nota è in modifica da " + lockOwnerOpt.get() + ". Impossibile ripristinare.");
        }

        List<VersioneNota> versioni = n.getVersioni();
        if (versioni == null || index < 0 || index >= versioni.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indice versione non valido");
        }

        VersioneNota v = versioni.get(index);

        // Salva versione corrente prima di ripristinare
        n.salvaVersionePrecedente();

        // Applica la versione
        n.setTitolo(v.getTitolo());
        n.setContenuto(v.getContenuto());
        n.setLastModifiedAt(LocalDateTime.now());
        n.setLastModifiedBy(userNorm);

        repo.save(n);
        LoggerActions.log("NOTE_RESTORE_SUCCESS", userNorm, Map.of(
                "noteId", id,
                "versionIndex", index));

    }

    // ============================================================
    // LOCKING — API logiche per il controller / frontend
    // ============================================================

    public String lock(int id, String username) {

        String userNorm = normalize(username);
        if (userNorm == null || userNorm.isBlank()) {
            LoggerActions.log("NOTE_LOCK_ERROR", "system", Map.of(
                    "noteId", id,
                    "reason", "username_non_valido"));
            return "error"; // username non valido
        }

        Note n = repo.findById(id);
        if (n == null) {
            LoggerActions.log("NOTE_LOCK_ERROR", userNorm, Map.of(
                    "noteId", id,
                    "reason", "nota_non_trovata"));
            return "not_found"; // nota non trovata
        }

        Optional<String> ownerOpt = repo.getEffectiveLockOwner(id);

        // Se non c’è un lock attivo o è scaduto, prova a prenderlo
        if (ownerOpt.isEmpty()) {
            boolean ok = repo.tryLock(id, userNorm);

            if (ok) {
                LoggerActions.log("NOTE_LOCK_RECOVERED", userNorm, Map.of(
                        "noteId", id));
                return "expired_recovered";
            } else {
                LoggerActions.log("NOTE_LOCK_ERROR", userNorm, Map.of(
                        "noteId", id,
                        "reason", "acquisizione_fallita"));
                return "error";
            }
        }

        String owner = ownerOpt.get();

        // Se il lock è già dell'utente, rinnova il lock
        if (owner.equalsIgnoreCase(userNorm)) {
            repo.refreshLock(id, userNorm);

            LoggerActions.log("NOTE_LOCK_RENEWED", userNorm, Map.of(
                    "noteId", id));
            return "locked";
        }

        LoggerActions.log("NOTE_LOCKED_BY_OTHER", userNorm, Map.of(
                "noteId", id,
                "currentOwner", owner));
        // Lock detenuto da un altro utente
        return "already_locked";
    }

    /** Restituisce l'effettivo proprietario del lock (solo se ancora valido). */
    public Optional<String> getLockOwner(int id) {
        return repo.getEffectiveLockOwner(id);
    }

    /** Refresh periodico del lock (chiamato dal frontend ogni X secondi/minuti). */
    public void refreshLockState(int id, String username) {
        String userNorm = normalize(username);
        if (userNorm != null && !userNorm.isBlank()) {
            repo.refreshLock(id, userNorm);
        }
    }

    /** Sblocco volontario. Ritorna "unlocked" o "forbidden". */
    public String unlock(int id, String username) {

        String userNorm = normalize(username);
        if (userNorm == null || userNorm.isBlank()) {
            LoggerActions.log("NOTE_UNLOCK_ERROR", "system", Map.of(
                    "noteId", id,
                    "reason", "username_non_valido"));
            return "forbidden";
        }

        boolean unlocked = repo.unlockNote(id, userNorm);

        // =============================
        // CASO 1 — Sblocco riuscito
        // =============================
        if (unlocked) {
            LoggerActions.log("NOTE_UNLOCK_SUCCESS", userNorm, Map.of(
                    "noteId", id));
            return "unlocked";
        }

        // =============================
        // CASO 2 — L'utente NON è il proprietario del lock
        // =============================
        LoggerActions.log("NOTE_UNLOCK_NOT_OWNER", userNorm, Map.of(
                "noteId", id,
                "reason", "tentativo_sblocco_non_autorizzato"));
        return "not_owner";
    }

    /** Stato del lock, usato da GET /lock */
    public Map<String, Object> getLockState(int id) {
        return repo.getLockState(id);
    }
}