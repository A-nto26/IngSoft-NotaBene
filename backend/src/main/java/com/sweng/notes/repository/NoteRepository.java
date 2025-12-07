package com.sweng.notes.repository;

import com.sweng.notes.model.Note;
import com.sweng.notes.model.Cartella;
import com.sweng.notes.model.Permesso;
import com.sweng.notes.model.Privata;
import com.sweng.notes.model.VersioneNota;

import org.mapdb.*;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * Repository MapDB robusto, null-safe e coerente
 * con le nuove regole di condivisione e versionamento.
 */
@Repository
public class NoteRepository {

    private final DB db;

    private final ConcurrentMap<Integer, Note> notes;
    private final ConcurrentMap<String, Cartella> folders;

    private int idCounter;

    private static final long LOCK_TIMEOUT_MINUTES = 10L;

    // Comparator sicuro contro createdAt null
    private static final Comparator<Note> NOTE_DATE_DESC = Comparator.comparing(
            Note::getCreatedAt,
            Comparator.nullsLast(Comparator.naturalOrder())).reversed();

    @SuppressWarnings("unchecked")
    public NoteRepository() {

        db = DBMaker.fileDB(new File("notes.db"))
                .fileMmapEnableIfSupported()
                .transactionEnable()
                .closeOnJvmShutdown()
                .make();

        notes = db.hashMap("notes", Serializer.INTEGER, Serializer.JAVA)
                .createOrOpen();

        folders = db.hashMap("folders", Serializer.STRING, Serializer.JAVA)
                .createOrOpen();

        idCounter = notes.isEmpty() ? 1 : Collections.max(notes.keySet()) + 1;

        normalizzaNote();
    }

    /**
     * Normalizza eventuali note vecchie (retrocompatibilità)
     */
    private void normalizzaNote() {
        for (Note n : notes.values()) {
            if (n.getUtentiCondivisi() == null) {
                n.setUtentiCondivisi(new LinkedHashSet<>());
            }
            if (n.getVersioni() == null) {
                n.setVersioni(new ArrayList<>());
            }
            if (n.getPermesso() == null) {
                n.setPermesso(new Privata());
            }
            if (n.getCreatedAt() == null) {
                n.setCreatedAt(LocalDateTime.now());
            }
            if (n.getLockedBy() == null)
                n.setLockedBy(null);

            if (n.getLockedAt() == null)
                n.setLockedAt(null);

            if (n.getColoreCartella() == null || n.getColoreCartella().isBlank()) {
                n.setColoreCartella("#ffb347"); // colore default
            }
        }
        commit();
    }

    // ============================================================
    // COSTRUTTORE PER I TEST (Sprint 4)
    // ============================================================
    public NoteRepository(DB testDb,
                        ConcurrentMap<Integer, Note> testNotes,
                        ConcurrentMap<String, Cartella> testFolders) {

        this.db = testDb;
        this.notes = testNotes;
        this.folders = testFolders;

        this.idCounter = notes.isEmpty()
                ? 1
                : Collections.max(notes.keySet()) + 1;

        normalizzaNote(); // Riusa la logica del repository reale
    }

    // ============================================================
    // UTILITY
    // ============================================================

    private synchronized int nextId() {
        return idCounter++;
    }

    private void commit() {
        db.commit();
    }

    // ============================================================
    // NOTE — CRUD
    // ============================================================

    public synchronized List<Note> findAll() {
        List<Note> all = new ArrayList<>(notes.values());
        all.sort(NOTE_DATE_DESC);
        return all;
    }

    public Note findById(int id) {
        return notes.get(id);
    }

    public synchronized void save(Note note) {
        boolean nuova = note.getId() == 0;

        if (nuova) {
            note.setId(nextId());
        }

        if (note.getUtentiCondivisi() == null) {
            note.setUtentiCondivisi(new LinkedHashSet<>());
        }
        if (note.getVersioni() == null) {
            note.setVersioni(new ArrayList<>());
        }
        if (note.getPermesso() == null) {
            note.setPermesso(new Privata());
        }
        if (note.getCreatedAt() == null) {
            note.setCreatedAt(LocalDateTime.now());
        }
        if (note.getColoreCartella() == null || note.getColoreCartella().isBlank()) {
            note.setColoreCartella("#ffb347");
        }

        notes.put(note.getId(), note);
        aggiornaCartella(note);

        commit();
    }

    // ============================================================
    // VERSIONAMENTO — RESTORE VERSION (Sprint 4)
    // ============================================================
    public synchronized void restoreVersion(int id, int index, String username) {

        Note n = notes.get(id);
        if (n == null)
            return;

        List<VersioneNota> versioni = n.getVersioni();
        if (versioni == null || index < 0 || index >= versioni.size())
            return;

        VersioneNota v = versioni.get(index);

        // Salva versione corrente come versione precedente
        n.salvaVersionePrecedente();

        // Applica i dati della versione
        n.setTitolo(v.getTitolo());
        n.setContenuto(v.getContenuto());
        n.setLastModifiedAt(LocalDateTime.now());
        n.setLastModifiedBy(username);

        notes.put(id, n);
        commit();
    }

    // ============================================================
    // CARTELLE — LOGICA COMPLETA SPRINT 4
    // ============================================================

    private void aggiornaCartella(Note n) {
        // 1. La nota NON ha una cartella → niente da fare
        if (n.getCartella() == null || n.getCartella().isBlank()) {
            return;
        }

        String key = n.getCartella().trim().toLowerCase();

        // 2. Recupera la cartella se esiste, altrimenti la crea
        Cartella c = folders.get(key);

        if (c == null) {
            // La nota ha un colore? → usalo
            String coloreCartella = (n.getColoreCartella() != null && !n.getColoreCartella().isBlank())
                    ? n.getColoreCartella()
                    : "#FFD700";

            c = new Cartella(n.getCartella().trim(), n.getCreatore(), coloreCartella);
            folders.put(key, c);
        }

        // 3. Sincronizzazione colore:
        // Se la nota ha un colore → aggiorna la cartella
        if (n.getColoreCartella() != null && !n.getColoreCartella().isBlank()) {
            c.setColore(n.getColoreCartella());
        }
        // Se la nota NON ha colore → eredita il colore della cartella
        else {
            n.setColoreCartella(c.getColore());
        }

        // 4. Associa ID nota alla cartella
        c.addNoteId(n.getId());

        // 5. Salva cartella e nota aggiornate
        folders.put(key, c);
        notes.put(n.getId(), n);
    }

    public synchronized void delete(int id) {
        Note n = notes.remove(id);

        if (n != null) {
            rimuoviNotaDallaCartella(n);
        }

        commit();
    }

    private void rimuoviNotaDallaCartella(Note n) {
        if (n.getCartella() == null)
            return;

        String key = n.getCartella().trim().toLowerCase();
        Cartella c = folders.get(key);

        if (c != null) {
            c.removeNoteId(n.getId());
            folders.put(key, c);
        }
    }

    // ============================================================
    // GESTIONE LOCK NOTE — VERSIONE COMPLETA
    // ============================================================

    /**
     * Prova a prendere il lock sulla nota.
     * Ritorna:
     * - true → lock acquisito
     * - false → lock detenuto da altro utente e ancora valido
     */
    public synchronized boolean tryLock(int id, String username) {
        Note n = notes.get(id);
        if (n == null)
            return false;

        // Se non c'è un lock attivo → lo prende subito
        if (!n.hasActiveLock()) {
            n.acquireLock(username);
            notes.put(id, n);
            commit();
            return true;
        }

        // Se il lock è scaduto → può "rubarlo"
        if (n.isLockExpired(LOCK_TIMEOUT_MINUTES)) {
            n.acquireLock(username);
            notes.put(id, n);
            commit();
            return true;
        }

        // Se il lock è suo → rinnova e OK
        if (n.isLockedBy(username)) {
            n.acquireLock(username); // aggiorna lockedAt
            notes.put(id, n);
            commit();
            return true;
        }

        // Lock ancora attivo di altro utente
        return false;
    }

    /**
     * Rinnova un lock già posseduto dallo stesso utente.
     * Se non è suo → nessun effetto.
     */
    public synchronized void refreshLock(int id, String username) {
        Note n = notes.get(id);
        if (n == null)
            return;

        if (n.isLockedBy(username)) {
            n.acquireLock(username);
            notes.put(id, n);
            commit();
        }
    }

    /**
     * Sblocca una nota SE:
     * - chi chiama è il proprietario del lock
     * - OPPURE il lock è scaduto
     */
    public synchronized void unlockIfOwnerOrExpired(int id, String username) {
        Note n = notes.get(id);
        if (n == null)
            return;

        boolean expired = n.isLockExpired(LOCK_TIMEOUT_MINUTES);
        boolean owner = n.isLockedBy(username);

        if (expired || owner) {
            n.clearLock();
            notes.put(id, n);
            commit();
        }
    }

    /**
     * Restituisce il proprietario del lock SE e solo SE il lock è ancora valido.
     * Se il lock è scaduto → lo rimuove e ritorna Optional.empty().
     */
    public synchronized Optional<String> getEffectiveLockOwner(int id) {
        Note n = notes.get(id);
        if (n == null)
            return Optional.empty();

        if (!n.hasActiveLock())
            return Optional.empty();

        if (n.isLockExpired(LOCK_TIMEOUT_MINUTES)) {
            n.clearLock();
            notes.put(id, n);
            commit();
            return Optional.empty();
        }

        return Optional.ofNullable(n.getLockedBy());
    }

    // ============================================================
    // ADAPTER COMPATIBILI CON IL SERVICE (lockNote, unlockNote, getLockState)
    // ============================================================

    /** Wrapper per NoteService.lock() */
    public synchronized boolean lockNote(int id, String username) {
        return tryLock(id, username);
    }

    /** Sblocca solo se owner o lock scaduto */
    public synchronized boolean unlockNote(int id, String username) {
        Note before = notes.get(id);
        if (before == null)
            return false;

        // usa la logica ufficiale di sblocco
        unlockIfOwnerOrExpired(id, username);

        Note after = notes.get(id);

        // se lockedBy è null → sbloccata correttamente
        return after.getLockedBy() == null;
    }

    /** Sblocco forzato — usato solo in casi particolari */
    public synchronized void forceUnlock(int id) {
        Note n = notes.get(id);
        if (n == null)
            return;

        n.clearLock();
        notes.put(id, n);
        commit();
    }

    /** Usato da GET /lock */
    public synchronized Map<String, Object> getLockState(int id) {
        Map<String, Object> out = new HashMap<>();
        Note n = notes.get(id);

        if (n == null) {
            out.put("locked", false);
            return out;
        }

        // lock non esiste o scaduto → rimuovi
        if (!n.hasActiveLock() || n.isLockExpired(LOCK_TIMEOUT_MINUTES)) {
            n.clearLock();
            notes.put(id, n);
            commit();
            out.put("locked", false);
            return out;
        }

        out.put("locked", true);
        out.put("lockedBy", n.getLockedBy());
        out.put("lockedAt", n.getLockedAt().toString());
        return out;
    }

    // ============================================================
    // CONDIVISIONE — REGOLE SPRINT 4
    // ============================================================

    /**
     * Aggiunge utenti alla condivisione (solo aggiunta)
     * — Usata dall’autore quando modifica la nota.
     */
    public synchronized void addUsersToShare(int id, Set<String> nuoviUtenti) {
       
        if (nuoviUtenti == null || nuoviUtenti.isEmpty())
            return;

        Note n = notes.get(id);
        if (n == null)
            return;

        Set<String> esistenti = n.getUtentiCondivisi();
        if (esistenti == null) {
            esistenti = new LinkedHashSet<>();
        }

        esistenti.addAll(nuoviUtenti);
        n.setUtentiCondivisi(esistenti);

        notes.put(id, n);
        commit();
    }

    /**
     * L’utente condiviso può rimuovere solo sé stesso
     */
    public synchronized void removeSelf(int id, String username) {
        Note n = notes.get(id);
        if (n == null)
            return;

        Set<String> condivisi = n.getUtentiCondivisi();
        if (condivisi != null) {
            condivisi.remove(username);
            n.setUtentiCondivisi(condivisi);
        }

        notes.put(id, n);
        commit();
    }

    /**
     * Condivisione iniziale alla creazione della nota.
     * (Il permesso non viene più modificato qui.)
     */
    public synchronized void shareNoteWithUsers(int id, Set<String> utenti, Permesso permesso) {
        if (utenti == null || utenti.isEmpty())
            return;

        Note n = notes.get(id);
        if (n == null)
            return;

        Set<String> condivisi = n.getUtentiCondivisi();
        if (condivisi == null) {
            condivisi = new LinkedHashSet<>();
        }

        condivisi.addAll(utenti);
        n.setUtentiCondivisi(condivisi);

        notes.put(id, n);
        commit();
    }

    public List<Note> findSharedWithUser(String username) {
        if (username == null || username.isBlank())
            return Collections.emptyList();

        List<Note> result = new ArrayList<>();

        for (Note n : notes.values()) {
            if (n != null && n.getUtentiCondivisi() != null && n.getUtentiCondivisi().contains(username)) {
                result.add(n);
            }
        }

        result.sort(NOTE_DATE_DESC);
        return result;
    }

    public List<Note> findByCreator(String username) {
        if (username == null || username.isBlank())
            return Collections.emptyList();

        List<Note> result = new ArrayList<>();

        for (Note n : notes.values()) {
            if (n != null && username.equalsIgnoreCase(n.getCreatore())) {
                result.add(n);
            }
        }

        result.sort(NOTE_DATE_DESC);
        return result;
    }

    // ============================================================
    // CARTELLE - API SPRINT 4
    // ============================================================

    public synchronized void createFolder(String nome, String creatore, String colore) {
        if (nome == null || nome.isBlank())
            return;

        String key = nome.trim().toLowerCase();

        if (folders.containsKey(key)) {
            throw new IllegalArgumentException("La cartella '" + nome + "' esiste già.");
        }

        if (colore == null || colore.isBlank())
            colore = "#FFD700";

        Cartella c = new Cartella(nome.trim(), creatore, colore);
        folders.put(key, c);
        commit();
    }

    public List<Cartella> findAllFolders() {
        return new ArrayList<>(folders.values());
    }

    public Cartella findFolderByName(String nome) {
        if (nome == null)
            return null;
        return folders.get(nome.trim().toLowerCase());
    }

    public synchronized void deleteFolder(String nome) {
        if (nome == null || nome.isBlank())
            return;

        String key = nome.trim().toLowerCase();
        Cartella c = folders.remove(key);

        if (c != null) {
            for (int noteId : c.getNoteIds()) {
                Note n = notes.get(noteId);
                if (n != null) {
                    n.setCartella(null);
                    notes.put(noteId, n);
                }
            }
        }

        commit();
    }

    public List<Note> findByCartella(String nomeCartellaRaw) {

        if (nomeCartellaRaw == null || nomeCartellaRaw.isBlank()) {
            return Collections.emptyList();
        }

        String key = nomeCartellaRaw.trim().toLowerCase();

        Cartella c = folders.get(key);
        if (c == null) {
            return Collections.emptyList();
        }

        List<Note> result = new ArrayList<>();
        for (int noteId : c.getNoteIds()) {
            Note n = notes.get(noteId);
            if (n != null) {
                result.add(n);
            }
        }

        result.sort(NOTE_DATE_DESC);
        return result;
    }

    public void close() {
        db.close();
    }
}