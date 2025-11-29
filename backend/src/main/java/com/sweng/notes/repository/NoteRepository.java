package com.sweng.notes.repository;

import com.sweng.notes.model.Note;
import com.sweng.notes.model.Cartella;
import com.sweng.notes.model.VersioneNota;
import com.sweng.notes.model.Privata;
import org.mapdb.*;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

@Repository
public class NoteRepository {

    private final DB db;
    private final ConcurrentMap<Integer, Note> notes;
    private final ConcurrentMap<String, Cartella> folders;

    private int idCounter;

    private static final Comparator<Note> NOTE_DATE_DESC = Comparator
            .comparing(Note::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .reversed();

    @SuppressWarnings("unchecked")
    public NoteRepository() {
        db = DBMaker.fileDB(new File("notes.db"))
                .fileMmapEnableIfSupported()
                .transactionEnable()
                .closeOnJvmShutdown()
                .make();

        notes = db.hashMap("notes", Serializer.INTEGER, Serializer.JAVA).createOrOpen();
        folders = db.hashMap("folders", Serializer.STRING, Serializer.JAVA).createOrOpen();

        idCounter = notes.isEmpty() ? 1 : Collections.max(notes.keySet()) + 1;

        normalizzaNote();
    }

    // Costruttore per test
    public NoteRepository(DB db, ConcurrentMap<Integer, Note> notes, ConcurrentMap<String, Cartella> folders) {
        this.db = db;
        this.notes = notes;
        this.folders = folders;
        this.idCounter = notes.isEmpty() ? 1 : Collections.max(notes.keySet()) + 1;
    }

    private void commit() {
        db.commit();
    }

    private synchronized int nextId() {
        return idCounter++;
    }

    private void normalizzaNote() {
        for (Note n : notes.values()) {
            if (n.getUtentiCondivisi() == null)
                n.setUtentiCondivisi(new LinkedHashSet<>());
            if (n.getVersioni() == null)
                n.setVersioni(new ArrayList<>());
            if (n.getPermesso() == null)
                n.setPermesso(new Privata());
            if (n.getCreatedAt() == null)
                n.setCreatedAt(LocalDateTime.now());
        }
        commit();
    }

    // ============================================================
    // CRUD
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
        boolean nuova = (note.getId() == 0);
        if (nuova) {
            note.setId(nextId());
        }

        if (note.getUtentiCondivisi() == null)
            note.setUtentiCondivisi(new LinkedHashSet<>());
        if (note.getVersioni() == null)
            note.setVersioni(new ArrayList<>());
        if (note.getPermesso() == null)
            note.setPermesso(new Privata());
        if (note.getCreatedAt() == null)
            note.setCreatedAt(LocalDateTime.now());

        notes.put(note.getId(), note);
        aggiornaCartella(note);
        commit();
    }

    private void aggiornaCartella(Note note) {
        if (note.getCartella() == null || note.getCartella().isBlank())
            return;

        String key = note.getCartella().trim().toLowerCase();
        Cartella c = folders.computeIfAbsent(key,
                k -> new Cartella(note.getCartella().trim(), note.getCreatore(), "#FFD700"));

        c.addNoteId(note.getId());
        folders.put(key, c);
    }

    public synchronized void delete(int id) {
        Note n = notes.remove(id);
        if (n != null) {
            rimuoviNotaDallaCartella(n);
        }

        if (n != null && n.getCartella() != null) {
            String key = n.getCartella().trim().toLowerCase();
            Cartella c = folders.get(key);
            if (c != null && c.getNoteIds().isEmpty()) {
                folders.remove(key);
            }
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
        }
    }

    // ============================================================
    // VERSIONAMENTO
    // ============================================================

    @SuppressWarnings("DuplicatedCode")
    public synchronized void restoreVersion(int id, int index, String username) {
        Note n = notes.get(id);
        if (n == null)
            return;

        List<VersioneNota> versioni = n.getVersioni();
        if (versioni == null || index < 0 || index >= versioni.size())
            return;

        // Preleva la versione da ripristinare
        VersioneNota restore = versioni.get(index);

        // Salva la versione attuale prima di sovrascrivere
        n.salvaVersionePrecedente();

        // Applica titolo e contenuto della versione scelta
        n.setTitolo(restore.getTitolo());
        n.setContenuto(restore.getContenuto());

        // Rimuove la versione ripristinata dalla lista
        versioni.remove(index);
        n.setVersioni(versioni);

        // Aggiorna metadati
        n.setLastModifiedAt(LocalDateTime.now());
        n.setLastModifiedBy(username);

        notes.put(id, n);
        commit();
    }

    // ============================================================
    // SHARING
    // ============================================================

    public synchronized void addUsersToShare(int id, Set<String> nuovi) {
        if (nuovi == null || nuovi.isEmpty())
            return;
        Note n = notes.get(id);
        if (n == null)
            return;

        Set<String> updated = n.getUtentiCondivisi();
        updated.addAll(nuovi);
        n.setUtentiCondivisi(updated);

        notes.put(id, n);
        commit();
    }

    public synchronized void removeSelf(int id, String user) {
        Note n = notes.get(id);
        if (n == null)
            return;

        Set<String> set = n.getUtentiCondivisi();
        set.remove(user);
        n.setUtentiCondivisi(set);

        notes.put(id, n);
        commit();
    }

    public List<Note> findSharedWithUser(String username) {
        if (username == null || username.isBlank())
            return List.of();

        List<Note> result = new ArrayList<>();
        for (Note n : notes.values()) {
            if (n.getUtentiCondivisi().contains(username)) {
                result.add(n);
            }
        }

        result.sort(NOTE_DATE_DESC);
        return result;
    }

    public List<Note> findByCreator(String username) {
        if (username == null || username.isBlank())
            return List.of();

        List<Note> result = new ArrayList<>();
        for (Note n : notes.values()) {
            if (username.equalsIgnoreCase(n.getCreatore())) {
                result.add(n);
            }
        }
        result.sort(NOTE_DATE_DESC);
        return result;
    }

    // ============================================================
    // CARTELLE
    // ============================================================

    public synchronized void createFolder(String nome, String creatore, String colore) {
        if (nome == null || nome.isBlank())
            return;
        String key = nome.trim().toLowerCase();

        if (folders.containsKey(key)) {
            throw new IllegalArgumentException("Cartella già esistente.");
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
            for (int id : c.getNoteIds()) {
                Note n = notes.get(id);
                if (n != null) {
                    n.setCartella(null);
                }
            }
        }

        commit();
    }

    public List<Note> findByCartella(String nome) {
        if (nome == null || nome.isBlank())
            return List.of();

        String key = nome.trim().toLowerCase();
        Cartella c = folders.get(key);
        if (c == null)
            return List.of();

        List<Note> result = new ArrayList<>();
        for (int id : c.getNoteIds()) {
            Note n = notes.get(id);
            if (n != null)
                result.add(n);
        }

        result.sort(NOTE_DATE_DESC);
        return result;
    }

    public void close() {
        db.close();
    }

    // ============================================================
    // LOCK SEMPLIFICATO — Sprint 3
    // ============================================================

    public synchronized boolean lockNote(int id, String username) {
        Note n = notes.get(id);
        if (n == null)
            return false;

        // Nessun lock attivo → assegna
        if (n.getLockedBy() == null) {
            n.setLockedBy(username);
            n.setLockedAt(LocalDateTime.now());
            notes.put(id, n);
            commit();
            return true;
        }

        // Stesso utente → ok
        if (username.equalsIgnoreCase(n.getLockedBy())) {
            return true;
        }

        // Altro utente → NON consentito nello Sprint 3
        return false;
    }

    public synchronized boolean unlockNote(int id, String username) {
        Note n = notes.get(id);
        if (n == null)
            return false;

        if (username.equalsIgnoreCase(n.getLockedBy())) {
            n.setLockedBy(null);
            n.setLockedAt(null);
            notes.put(id, n);
            commit();
            return true;
        }
        return false;
    }

    public synchronized void forceUnlock(int id) {
        Note n = notes.get(id);
        if (n == null)
            return;

        n.setLockedBy(null);
        n.setLockedAt(null);
        notes.put(id, n);
        commit();
    }

    public Map<String, Object> getLockState(int id) {
        Note n = notes.get(id);
        if (n == null)
            return Map.of();

        if (n.getLockedBy() == null) {
            return Map.of();
        }

        return Map.of(
                "lockedBy", n.getLockedBy(),
                "lockedAt", n.getLockedAt());
    }
}