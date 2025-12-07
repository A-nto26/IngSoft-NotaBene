package com.sweng.notes.service;

import com.sweng.notes.dto.UserResponse;
import com.sweng.notes.model.Utente;
import com.sweng.notes.repository.UserRepository;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

    @Autowired
    public UserService(UserRepository userRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.encoder = encoder;
    }

    // ============================================================
    // REGISTER - Sprint4 (con UserResponse)
    // ============================================================
    public UserResponse register(String username, String password) {

        if (username == null || username.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Username obbligatorio");

        if (password == null || password.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password obbligatoria");

        String normalized = username.trim().toLowerCase();

        if (!USERNAME_PATTERN.matcher(normalized).matches())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Username non valido (solo lettere, numeri, underscore, 3-20 caratteri)");

        if (password.length() < 8)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La password deve contenere almeno 8 caratteri");

        if (userRepo.exists(username))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nome utente già registrato");

        // Hash e salvataggio
        String hash = encoder.encode(password);
         Utente utente = new Utente(normalized, hash);
        userRepo.save(utente);

        return new UserResponse(true, "Registrazione completata", username);
    }

    // ============================================================
    // LOGIN - Sprint4 (UserResponse)
    // ============================================================
    public UserResponse login(String username, String password) {

        if (username == null || password == null)
            return new UserResponse(false, "Credenziali non valide", null);

        username = username.trim().toLowerCase();

        Utente u = userRepo.findByUsername(username);
        if (u == null)
            return new UserResponse(false, "Utente non registrato", null);

        if (!encoder.matches(password, u.getPasswordHash()))
            return new UserResponse(false, "Password errata", null);

        return new UserResponse(true, "Login effettuato", username);
    }

    // ============================================================
    //  LETTURA UTENTI 
    // ============================================================
    /** Restituisce tutti gli utenti registrati. */
    public Collection<Utente> getAllUsers() {
        return userRepo.findAll();
    }

    /** Restituisce solo gli username, ordinati alfabeticamente. */
    public Collection<String> getAllUsernames() {
        return userRepo.findAll().stream()
                .map(Utente::getUsername)
                .sorted()
                .collect(Collectors.toList());
    }

    /** Verifica se un utente esiste. */
    public boolean exists(String username) {
        return userRepo.exists(username);
    }
}
