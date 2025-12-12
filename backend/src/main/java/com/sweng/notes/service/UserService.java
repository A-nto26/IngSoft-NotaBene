package com.sweng.notes.service;

import com.sweng.notes.dto.UserResponse;
import com.sweng.notes.model.Utente;
import com.sweng.notes.repository.UserRepository;
import com.sweng.notes.logging.LoggerActions;

import java.util.Collection;
import java.util.Map;
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

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

    @Autowired
    public UserService(UserRepository userRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.encoder = encoder;
    }

    // ============================================================
    // REGISTER
    // ============================================================
    public UserResponse register(String username, String password) {

        if (username == null || username.isBlank()) {
            LoggerActions.log("USER_REGISTER_FAIL", "<invalid_username>", Map.of(
                    "reason", "username_mancante"));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Username obbligatorio");
        }

        String normalized = username.trim().toLowerCase();

        if (password == null || password.isBlank()) {
            LoggerActions.log("USER_REGISTER_FAIL", normalized, Map.of(
                    "reason", "password_mancante"));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password obbligatoria");
        }

        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            LoggerActions.log("USER_REGISTER_FAIL", normalized, Map.of(
                    "reason", "username_formato_non_valido"));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Username non valido (solo lettere, numeri, underscore, 3-20 caratteri)");
        }

        if (password.length() < 8) {
            LoggerActions.log("USER_REGISTER_FAIL", normalized, Map.of(
                    "reason", "password_troppo_corta"));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La password deve contenere almeno 8 caratteri");
        }

        if (userRepo.exists(normalized)) {
            LoggerActions.log("USER_REGISTER_FAIL", normalized, Map.of(
                    "reason", "username_gia_registrato"));
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Nome utente già registrato");
        }

        // Hash e salvataggio
        String hash = encoder.encode(password);
        Utente utente = new Utente(normalized, hash);
        userRepo.save(utente);
        LoggerActions.log("USER_REGISTER_SUCCESS", normalized, Map.of());

        return new UserResponse(true, "Registrazione completata", normalized);
    }

    // ============================================================
    // LOGIN
    // ============================================================
    public UserResponse login(String username, String password) {

        if (username == null || password == null) {
            LoggerActions.log("USER_LOGIN_FAIL", "system", Map.of(
                    "reason", "credenziali_mancanti"));
            return new UserResponse(false, "Credenziali non valide", null);
        }

        username = username.trim().toLowerCase();

        Utente u = userRepo.findByUsername(username);
        if (u == null) {
            LoggerActions.log("USER_LOGIN_FAIL", username, Map.of(
                    "reason", "utente_non_registrato"));
            return new UserResponse(false, "Utente non registrato", null);
        }

        if (!encoder.matches(password, u.getPasswordHash())) {
            LoggerActions.log("USER_LOGIN_FAIL", username, Map.of(
                    "reason", "password_errata"));
            return new UserResponse(false, "Password errata", null);
        }

        LoggerActions.log("USER_LOGIN_SUCCESS", username, Map.of());

        return new UserResponse(true, "Login effettuato", username);
    }

    // ============================================================
    // LETTURA UTENTI
    // ============================================================
    /** Restituisce tutti gli utenti registrati. */
    public Collection<Utente> getAllUsers() {
        return userRepo.findAll();
    }

    /** Restituisce solo gli username, ordinati alfabeticamente. */
    public Collection<String> getAllUsernames() {
        return userRepo.findAll().stream()
                .map(u -> u.getUsername().trim().toLowerCase())
                .sorted()
                .collect(Collectors.toList());
    }

    /** Verifica se un utente esiste. */
    public boolean exists(String username) {
        return userRepo.exists(username);
    }
}
