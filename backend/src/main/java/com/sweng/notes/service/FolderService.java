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
    // OPERAZIONI BASE
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

        String nome = nomeCartella.trim();

        // Controllo PR-friendly per cartelle duplicate
        if (noteRepo.findFolderByName(nome) != null) {
            throw new IllegalArgumentException("La cartella '" + nome + "' esiste già.");
        }

        noteRepo.createFolder(nome, creatore, colore);
    }

    /** Elimina una cartella esistente (e dissocia le note) */
    public void deleteFolder(String nomeCartella) {
        if (nomeCartella == null || nomeCartella.isBlank())
            return;

        noteRepo.deleteFolder(nomeCartella.trim().toLowerCase());
    }

    // ============================================================
    // NOTE VISIBILI PER UTENTE (SPRINT 3)
    // ============================================================

    /**
     * Restituisce le note visibili per un utente in una cartella:
     *  - autore → sempre visibile
     *  - condivise → visibili solo se n.puoLeggere(username)
     */
    public List<Note> getNotesInFolderForUser(String nomeCartella, String username) {

        if (nomeCartella == null || nomeCartella.isBlank()
                || username == null || username.isBlank()) {
            return Collections.emptyList();
        }

        String folderKey = nomeCartella.trim().toLowerCase();

        List<Note> tutte = noteRepo.findByCartella(folderKey);
        List<Note> visibili = new ArrayList<>();

        for (Note n : tutte) {
            if (username.equalsIgnoreCase(n.getCreatore())) {
                visibili.add(n);
            } 
            else if (n.puoLeggere(username)) {  // ⭐ MIGLIORIA Sprint 3
                visibili.add(n);
            }
        }

        return visibili;
    }
}
