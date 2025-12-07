package com.sweng.notes.service;

import com.sweng.notes.dto.*;
import com.sweng.notes.model.*;
import com.sweng.notes.repository.NoteRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Servizio principale per la gestione delle note.
 * Versione Sprint 4:
 * - Visibilità note (mie + condivise)
 * - Versionamento (salvaVersionePrecedente + restore)
 * - Cartelle + colore cartella
 * - Condivisione con nuove regole (autore aggiunge, utenti rimuovono solo sé stessi)
 * - Lock concorrente completo (timeout, recupero, unlock condizionato)
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
    // CREATE — UC4
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

        // Permesso: impostato SOLO in creazione (regola Sprint 4)
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

        // Colore cartella (se presente nel DTO; altrimenti ci pensa il repository)
        if (req.getColoreCartella() != null && !req.getColoreCartella().isBlank()) {
            n.setColoreCartella(req.getColoreCartella().trim());
        } else {
            n.setColoreCartella("#ffb347"); 
        }

        repo.save(n);
        return n;
    }

    // ============================================================
    // GET VISIBLE — UC3
    // ============================================================
    public List<Note> getVisibleNotesForUser(String username) {
        return getNotesForUserFiltered(username, true, true);
    }

    public List<Note> getNotesForUserFiltered(
            String username,
            boolean mie,
            boolean condivise) {

        String norm = normalize(username);
        if (norm == null || username.isBlank()) {
            throw new IllegalArgumentException("Username non può essere nullo o vuoto");
        }

        Set<Note> result = new LinkedHashSet<>();

        if (mie)
            result.addAll(repo.findByCreator(norm));

        if (condivise)
            result.addAll(repo.findSharedWithUser(norm));

        return List.copyOf(result);
    }

    /* ===== PER TEST ===== */
    public List<Note> getNotesForUser(String username) {
        return getVisibleNotesForUser(username);
    }

    public List<Note> getAllNotes() {
        return repo.findAll();
    }

    public Note getNoteById(int id) {
        return repo.findById(id);
    }

    public List<Note> getNotesByCartella(String cartella) {
        return repo.findByCartella(cartella);
    }

    // ============================================================
    // UPDATE — UC10 (con lock + versioning + permessi Sprint 4)
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

        // 1) Permessi: Sprint 4 → chi può scrivere? (autore o utenti con permesso SCRITTURA)
        if (!n.puoScrivere(userNorm)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non hai i permessi per modificare questa nota");
        }

        // 2) Lock: usiamo getEffectiveLockOwner del repository (lock attivo e non scaduto)
        Optional<String> lockOwnerOpt = repo.getEffectiveLockOwner(id);

        if (lockOwnerOpt.isEmpty()) {
            // lock scaduto o inesistente → conflitto 409
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lock scaduto. Riapri la nota per continuare.");
        }

        String lockOwner = lockOwnerOpt.get();
        if (!lockOwner.equalsIgnoreCase(userNorm)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La nota è in modifica da " + lockOwner + "."
            );
        }

        // 3) Lock valido → salva versione → aggiorna
        n.salvaVersionePrecedente();

        if (req.getTitolo() != null && !req.getTitolo().isBlank()) {
            n.setTitolo(req.getTitolo());
        }

        if (req.getContenuto() != null && !req.getContenuto().isBlank()) {
            n.setContenuto(req.getContenuto());
        }

        if (req.getCartella() != null) {
            n.setCartella(req.getCartella());
        }

        if (req.getColoreCartella() != null && !req.getColoreCartella().isBlank()) {
            n.setColoreCartella(req.getColoreCartella().trim());
        }

        n.setLastModifiedAt(LocalDateTime.now());
        n.setLastModifiedBy(userNorm);

        repo.save(n);
        return n;
    }

    /* ===== PER TEST ===== */
    public void save(Note note) {
        if (note != null)
            repo.save(note);
    }

    // ============================================================
    // DELETE — UC12
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

        // Solo l'autore può eliminare
        if (!Objects.equals(n.getCreatore(), userNorm)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato");
        }

        repo.delete(id);
    }

    // ============================================================
    // DUPLICATE — UC6
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

        // Sprint 4: può duplicare chi può leggere
        if (!orig.puoLeggere(userNorm)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato");
        }

        Note copia = new Note(
                0,
                orig.getTitolo() + " (Copia)",
                orig.getContenuto(),
                userNorm,
                orig.getCartella()
        );

        // Regole Sprint 4: copia PRIVATA, senza condivisioni, versioni vuote
        copia.setPermesso(new Privata());
        copia.setVersioni(new ArrayList<>());
        copia.setUtentiCondivisi(new LinkedHashSet<>());
        copia.setColoreCartella(orig.getColoreCartella());
        copia.setCreatedAt(LocalDateTime.now());
        copia.setLastModifiedAt(LocalDateTime.now());
        copia.setLastModifiedBy(userNorm);

        repo.save(copia);
        return copia;
    }

    // ============================================================
    // REMOVE SELF — UC7
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

        // Il repository implementa la regola "può rimuovere solo sé stesso"
        repo.removeSelf(id, userNorm);
    }

    // ============================================================
    // SEARCH — UC8
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
    // SET CARTELLA — UC9
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

        // Solo autore può cambiare cartella
        if (!Objects.equals(n.getCreatore(), userNorm)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato");
        }

        n.salvaVersionePrecedente();
        n.setCartella(nuovoNome);
        n.setLastModifiedAt(LocalDateTime.now());
        n.setLastModifiedBy(userNorm);

        repo.save(n);
    }

    // ============================================================
    // SHARE — UC11
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

        // Regola Sprint 4: il permesso non viene modificato qui
        repo.addUsersToShare(id, new LinkedHashSet<>(req.getUtentiCondivisi()));
    }

    /* ===== PER TEST ===== */
    public void addUsersToShare(int id, Set<String> nuovi) {
        if (nuovi == null || nuovi.isEmpty()) return;
        repo.addUsersToShare(id, nuovi);
    }

    /* ===== PER TEST ===== */
    public List<Note> getSharedNotes(String username) {
        String norm = normalize(username);
        if (norm == null)
            return List.of();
        return repo.findSharedWithUser(norm);
    }

    /* ===== PER TEST ===== */
    public List<Note> getNotesByCreator(String username) {
        String norm = normalize(username);
        if (norm == null)
            return List.of();
        return repo.findByCreator(norm);
    }

    // ============================================================
    // RESTORE VERSION — UC5
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

        // Autore o utente con permesso SCRITTURA
        if (!Objects.equals(n.getCreatore(), userNorm) && !n.puoScrivere(userNorm)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato");
        }

        // Controllo lock: se il lock è di un altro utente ancora valido → 409
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
    }

    // ============================================================
    // LOCKING — API logiche per il controller / frontend
    // ============================================================

    /**
     * Richiesta lock.
     * Ritorna:
     * - "locked": lock confermato (già tuo)
     * - "already_locked": lock di altro utente ancora valido
     * - "expired_recovered": lock scaduto, ora recuperato da te
     * - "not_found": nota inesistente
     */
    public String lock(int id, String username) {

        String userNorm = normalize(username);
        if (userNorm == null || userNorm.isBlank()) {
            return "error";
        }

        Note n = repo.findById(id);
        if (n == null) {
            return "not_found";
        }

        Optional<String> ownerOpt = repo.getEffectiveLockOwner(id);

        // Nessun lock attivo o lock scaduto → provo ad acquisirlo
        if (ownerOpt.isEmpty()) {
            boolean ok = repo.tryLock(id, userNorm);
            return ok ? "expired_recovered" : "error";
        }

        String owner = ownerOpt.get();

        // Lock è mio → rinnovo
        if (owner.equalsIgnoreCase(userNorm)) {
            repo.refreshLock(id, userNorm);
            return "locked";
        }

        // Lock di altro utente → bloccato
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
            return "forbidden";
        }

        boolean unlocked = repo.unlockNote(id, userNorm);
        return unlocked ? "unlocked" : "forbidden";
    }

    /** Stato del lock, usato da GET /lock */
    public Map<String, Object> getLockState(int id) {
        return repo.getLockState(id);
    }
}