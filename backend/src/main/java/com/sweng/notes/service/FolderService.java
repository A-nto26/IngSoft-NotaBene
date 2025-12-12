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
    // UTILITY INTERNA PER NORMALIZZARE
    // ============================================================
    private String normalize(String s) {
        return (s == null) ? null : s.trim().toLowerCase();
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
        String key = normalize(nomeCartella);
        if (key == null || key.isBlank()) {
            return Collections.emptyList();
        }
        return noteRepo.findByCartella(key);
    }

    /** Crea una nuova cartella */
    public void createFolder(String nomeCartella, String colore, String creatore) {

        String nomeNorm = normalize(nomeCartella);
        String creatoreNorm = normalize(creatore);

        if (nomeNorm == null || nomeNorm.isBlank()) {
            LoggerActions.log("FOLDER_CREATE_FAIL",
                    creatoreNorm != null ? creatoreNorm : "system",
                    Map.of("reason", "nome_mancante"));
            throw new IllegalArgumentException("Nome cartella obbligatorio");
        }

        if (noteRepo.findFolderByName(nomeNorm) != null) {
            LoggerActions.log("FOLDER_CREATE_FAIL", creatoreNorm, Map.of(
                    "folder", nomeNorm,
                    "reason", "cartella_esistente"));
            throw new IllegalArgumentException("La cartella '" + nomeNorm + "' esiste già.");
        }

        String coloreEffettivo = (colore == null || colore.isBlank())
                ? "#FFD700"
                : colore.trim();

        noteRepo.createFolder(nomeNorm, creatoreNorm, coloreEffettivo);

        LoggerActions.log("FOLDER_CREATE_SUCCESS", creatoreNorm, Map.of(
                "folder", nomeNorm,
                "color", coloreEffettivo));
    }

    /** Elimina una cartella esistente (e dissocia le note) */
    public void deleteFolder(String nomeCartella) {

        String nomeNorm = normalize(nomeCartella);
        if (nomeNorm == null || nomeNorm.isBlank()) {
            LoggerActions.log("FOLDER_DELETE_FAIL", "system", Map.of("reason", "nome_mancante"));
            return;
        }

        noteRepo.deleteFolder(nomeNorm);

        LoggerActions.log("FOLDER_DELETE_SUCCESS", "system", Map.of(
                "folder", nomeNorm));
    }

    // ============================================================
    // NOTE VISIBILI PER UTENTE (PROPRIE + CONDIVISE)
    // ============================================================

    public List<Note> getNotesInFolderForUser(String nomeCartella, String username) {

        String folderKey = normalize(nomeCartella);
        String userNorm = normalize(username);

        if (folderKey == null || folderKey.isBlank() ||
            userNorm == null || userNorm.isBlank()) {

            LoggerActions.log("FOLDER_NOTES_VIEW_FAIL",
                    userNorm != null ? userNorm : "system",
                    Map.of("reason", "parametri_invalidi"));
            return Collections.emptyList();
        }

        List<Note> tutte = noteRepo.findByCartella(folderKey);
        List<Note> visibili = new ArrayList<>();

        for (Note n : tutte) {
            if (n.puoLeggere(userNorm)) {
                visibili.add(n);
            }
        }

        LoggerActions.log("FOLDER_NOTES_VIEW_SUCCESS", userNorm, Map.of(
                "folder", folderKey,
                "notesReturned", visibili.size()));

        return visibili;
    }
}
