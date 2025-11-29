package com.sweng.notes.service;

import com.sweng.notes.dto.*;
import com.sweng.notes.model.*;
import com.sweng.notes.repository.NoteRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class NoteService {

    private final NoteRepository repo;

    public NoteService(NoteRepository repo) {
        this.repo = repo;
    }

    // ============================================================
    // CREATE — UC4
    // ============================================================
    public Note create(CreateNoteRequest req) {

        Note n = new Note(
                0,
                req.getTitolo(),
                req.getContenuto(),
                req.getCreatore().trim().toLowerCase(),
                req.getCartella());

        Permesso p;
        if ("LETTURA".equalsIgnoreCase(req.getPermesso())) {
            p = new Lettura();
        } else if ("SCRITTURA".equalsIgnoreCase(req.getPermesso())) {
            p = new Scrittura();
        } else {
            p = new Privata();
        }
        n.setPermesso(p);

        if (p instanceof Privata) {
            n.setUtentiCondivisi(new LinkedHashSet<>());
        } else if (req.getUtentiCondivisi() != null) {
            n.setUtentiCondivisi(new LinkedHashSet<>(req.getUtentiCondivisi()));
        } else {
            n.setUtentiCondivisi(new LinkedHashSet<>());
        }

        n.setVersioni(new ArrayList<>());
        repo.save(n);
        return n;
    }

    // ============================================================
    // GET VISIBLE — UC3
    // ============================================================
    public List<Note> getVisibleNotes(String username) {
        username = username.trim().toLowerCase();

        List<Note> mie = repo.findByCreator(username);
        List<Note> condivise = repo.findSharedWithUser(username);

        List<Note> tot = new ArrayList<>();
        tot.addAll(mie);
        tot.addAll(condivise);
        return tot;
    }

    // ============================================================
    // UPDATE — UC10
    // ============================================================
    public Note update(int id, NoteUpdateRequest req, String username) {

        username = username.trim().toLowerCase();

        Note n = repo.findById(id);
        if (n == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota non trovata");

        if (!Objects.equals(n.getCreatore(), username))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato");

        if (n.getLockedBy() != null
                && !n.getLockedBy().equalsIgnoreCase(username)
                && !isLockExpired(n)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Nota attualmente modificata da " + n.getLockedBy());
        }

        n.salvaVersionePrecedente();

        if (req.getTitolo() != null && !req.getTitolo().isBlank())
            n.setTitolo(req.getTitolo());

        if (req.getContenuto() != null && !req.getContenuto().isBlank())
            n.setContenuto(req.getContenuto());

        if (req.getCartella() != null)
            n.setCartella(req.getCartella());

        n.setLastModifiedAt(LocalDateTime.now());
        n.setLastModifiedBy(username);

        repo.save(n);
        return n;
    }

    private boolean isLockExpired(Note n) {
        if (n.getLockedAt() == null)
            return true;
        return n.getLockedAt().plusMinutes(10).isBefore(LocalDateTime.now());
    }

    // ============================================================
    // DELETE — UC12
    // ============================================================
    public void delete(int id, String username) {

        username = username.trim().toLowerCase();

        Note n = repo.findById(id);
        if (n == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota non trovata");

        if (!Objects.equals(n.getCreatore(), username))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato");

        repo.delete(id);
    }

    // ============================================================
    // DUPLICATE — UC6
    // ============================================================
    public Note duplicate(int id, String username) {

        username = username.trim().toLowerCase();

        Note orig = repo.findById(id);
        if (orig == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota non trovata");

        if (!orig.puoLeggere(username))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato");

        Note copia = new Note(
                0,
                orig.getTitolo() + " (Copia)",
                orig.getContenuto(),
                username,
                orig.getCartella());

        copia.setPermesso(new Privata());
        copia.setVersioni(new ArrayList<>());
        copia.setUtentiCondivisi(new LinkedHashSet<>());

        repo.save(copia);
        return copia;
    }

    // ============================================================
    // REMOVE SELF — UC7
    // ============================================================
    public void removeSelf(int id, String username) {

        username = username.trim().toLowerCase();

        Note n = repo.findById(id);
        if (n == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota non trovata");

        if (!n.getUtentiCondivisi().contains(username))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non condivisa con te");

        repo.removeSelf(id, username);
    }

    // ============================================================
    // SEARCH — UC8
    // ============================================================
    public List<Note> search(String username, String query) {

        username = username.trim().toLowerCase();

        List<Note> visibili = getVisibleNotes(username);

        if (query == null || query.isBlank())
            return visibili;

        String q = query.toLowerCase();
        List<Note> result = new ArrayList<>();

        for (Note n : visibili) {
            if (n.getTitolo().toLowerCase().contains(q)
                    || n.getContenuto().toLowerCase().contains(q)) {
                result.add(n);
            }
        }

        return result;
    }

    // ============================================================
    // SET CARTELLA — UC9
    // ============================================================
    public void setCartella(int id, String nuovoNome, String username) {

        username = username.trim().toLowerCase();

        Note n = repo.findById(id);
        if (n == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota non trovata");

        if (!Objects.equals(n.getCreatore(), username))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato");

        n.salvaVersionePrecedente();
        n.setCartella(nuovoNome);
        n.setLastModifiedAt(LocalDateTime.now());
        n.setLastModifiedBy(username);

        repo.save(n);
    }

    // ============================================================
    // SHARE — UC11
    // ============================================================
    public void shareNote(int id, ShareNoteRequest req, String autore) {

        autore = autore.trim().toLowerCase();

        Note n = repo.findById(id);
        if (n == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota non trovata");

        if (!Objects.equals(n.getCreatore(), autore))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato");

        if (req.getUtentiCondivisi() == null || req.getUtentiCondivisi().isEmpty())
            return;

        repo.addUsersToShare(id,
                new LinkedHashSet<>(req.getUtentiCondivisi()));
    }

    // ============================================================
    // RESTORE VERSION — UC5
    // ============================================================
    public void restoreVersion(int id, int index, String username) {

        username = username.trim().toLowerCase();

        Note n = repo.findById(id);
        if (n == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota non trovata");

        if (!Objects.equals(n.getCreatore(), username)
                && !n.puoScrivere(username))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato");

        repo.restoreVersion(id, index, username);
    }

}
