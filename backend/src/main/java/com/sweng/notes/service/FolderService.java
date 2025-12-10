package com.sweng.notes.service;

import com.sweng.notes.model.Note;
import com.sweng.notes.model.Cartella;
import com.sweng.notes.repository.NoteRepository;
import org.springframework.stereotype.Service;
import com.sweng.notes.logging.LoggerActions;

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

    /** Restituisce tutte le cartelle esistenti nel sistema */
    public List<Cartella> getAllFolders() {
        return noteRepo.findAllFolders();
    }

    /** Restituisce tutte le note appartenenti ad una cartella */
    public List<Note> getNotesInFolder(String nomeCartella) {
        if (nomeCartella == null || nomeCartella.isBlank()) {
            return Collections.emptyList();
        }
        return noteRepo.findByCartella(nomeCartella.trim().toLowerCase());
    }

    /** Crea una nuova cartella */
    public void createFolder(String nomeCartella, String colore, String creatore) {
        if (nomeCartella == null || nomeCartella.isBlank()) {
            LoggerActions.log("FOLDER_CREATE_FAIL", 
                creatore != null ? creatore : "system",
                Map.of("reason", "nome_mancante"));
            throw new IllegalArgumentException("Nome cartella obbligatorio");
        }

        String nomeNorm = nomeCartella.trim().toLowerCase();
        String creatoreNorm = creatore != null ? creatore.trim().toLowerCase() : null;

        if (noteRepo.findFolderByName(nomeNorm) != null) {
            LoggerActions.log("FOLDER_CREATE_FAIL", creatoreNorm, Map.of(
                "folder", nomeNorm,
                "reason", "cartella_esistente"
            ));
            throw new IllegalArgumentException("La cartella '" + nomeNorm + "' esiste già.");
        }

        String coloreEffettivo = (colore == null || colore.isBlank())
                ? "#FFD700"
                : colore;

        noteRepo.createFolder(nomeNorm, creatoreNorm, coloreEffettivo);
        LoggerActions.log("FOLDER_CREATE_SUCCESS", creatoreNorm, Map.of(
            "folder", nomeNorm,
            "color", coloreEffettivo
        ));
    }

    /** Elimina una cartella esistente (e dissocia le note) */
    public void deleteFolder(String nomeCartella) {
        if (nomeCartella == null || nomeCartella.isBlank()) {
            LoggerActions.log("FOLDER_DELETE_FAIL", "system", Map.of(
                "reason", "nome_mancante"
            ));
            return;
        }

        String nomeNorm = nomeCartella.trim().toLowerCase();

        noteRepo.deleteFolder(nomeNorm);

        LoggerActions.log("FOLDER_DELETE_SUCCESS", "system", Map.of(
            "folder", nomeNorm
        ));
    }

    // ============================================================
    // NOTE VISIBILI PER UTENTE (PROPRIE + CONDIVISE)
    // ============================================================

    /**
     * Restituisce tutte le note visibili per un utente in una specifica cartella.
     * La visibilità si basa su:
     * - autore
     * - utenti con cui la nota è condivisa
     * - permesso associato alla nota (può leggere / può scrivere)
     */
    public List<Note> getNotesInFolderForUser(String nomeCartella, String username) {

        if (nomeCartella == null || nomeCartella.isBlank() ||
                username == null || username.isBlank()) {

             LoggerActions.log("FOLDER_NOTES_VIEW_FAIL", 
                username != null ? username : "system", 
                Map.of("reason", "parametri_invalidi"));

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

        LoggerActions.log("FOLDER_NOTES_VIEW_SUCCESS", userNorm, Map.of(
            "folder", folderKey,
            "notesReturned", visibili.size()
        ));
        
        return visibili;
    }
}
