package com.sweng.notes.service;

import com.sweng.notes.model.Note;
import com.sweng.notes.model.Cartella;
import com.sweng.notes.repository.NoteRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FolderService {

    private final NoteRepository noteRepo;

    public FolderService(NoteRepository noteRepo) {
        this.noteRepo = noteRepo;
    }

    // ============================================================
    // CARTELLE - OPERAZIONI BASE
    // ============================================================

    /** Restituisce tutte le cartelle esistenti */
    public List<Cartella> getAllFolders() {
        return noteRepo.findAllFolders();
    }

    /** Restituisce tutte le note di una cartella */
    public List<Note> getNotesInFolder(String nomeCartella) {
        if (nomeCartella == null || nomeCartella.isBlank()) {
            return Collections.emptyList();
        }
        return noteRepo.findByCartella(nomeCartella.trim().toLowerCase());
    }

    /** Crea una nuova cartella */
    public void createFolder(String nomeCartella, String colore, String creatore) {
        if (nomeCartella == null || nomeCartella.isBlank()) {
            throw new IllegalArgumentException("Nome cartella obbligatorio");
        }

        String nomeNorm = nomeCartella.trim().toLowerCase();
        String creatoreNorm = creatore != null ? creatore.trim().toLowerCase() : null;

        // Controllo PR-friendly per cartelle duplicate
        if (noteRepo.findFolderByName(nomeNorm) != null) {
            throw new IllegalArgumentException("La cartella '" + nomeNorm + "' esiste già.");
        }

        String coloreEffettivo = (colore == null || colore.isBlank())
                ? "#FFD700"
                : colore;

        noteRepo.createFolder(nomeNorm, creatoreNorm, coloreEffettivo);
    }

    /** Elimina una cartella esistente (e dissocia le note) */
    public void deleteFolder(String nomeCartella) {
        if (nomeCartella == null || nomeCartella.isBlank())
            return;

        noteRepo.deleteFolder(nomeCartella.trim().toLowerCase());
    }

    // ============================================================
    // NOTE VISIBILI PER UTENTE (PROPRIE + CONDIVISE)
    // ============================================================

    /**
     * Restituisce tutte le note in una determinata cartella
     * visibili per un utente (regola Sprint 4).
     * usa: note.puoLeggere(username)
     */
    public List<Note> getNotesInFolderForUser(String nomeCartella, String username) {

        if (nomeCartella == null || nomeCartella.isBlank() || 
            username == null || username.isBlank()) {
                return Collections.emptyList();
        }

        String folderKey = nomeCartella.trim().toLowerCase();
        String userNorm = username.trim().toLowerCase();

        List<Note> tutte = noteRepo.findByCartella(folderKey);
        List<Note> visibili = new ArrayList<>();

        for (Note n : tutte) {
            if (n.puoLeggere(userNorm)) {
                visibili.add(n);
            }
        }

        return visibili;
    }
}
