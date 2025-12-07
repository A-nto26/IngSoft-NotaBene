package com.sweng.notes.controller;

import com.sweng.notes.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.sweng.notes.dto.UserResponse;

import java.util.Collection;

/**
 * Controller REST per la gestione degli utenti.
 * - Registrazione con username + password
 * - Login
 * - Elenco username (per condivisione note)
 */
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
        RequestMethod.GET,
        RequestMethod.POST,
        RequestMethod.PUT,
        RequestMethod.DELETE,
        RequestMethod.OPTIONS
})
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // DTO semplice per richieste di registrazione/login
    public static class UserRequest {
        private String username;
        private String password;

        public UserRequest() {
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    // ============================================================
    // REGISTRAZIONE
    // ============================================================
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody UserRequest req) {
        UserResponse response = userService.register(
            req.getUsername(),
            req.getPassword()
        );

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // LOGIN
    // ============================================================
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@RequestBody UserRequest req) {

        UserResponse resp = userService.login(req.getUsername(), req.getPassword());

        if (!resp.isSuccess()) {

            // Utente non esistente → 404
            if ("Utente non registrato".equals(resp.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
            }

            // Password errata → 401
            if ("Password errata".equals(resp.getMessage())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
            }

            // Fallback, in caso di altri messaggi
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
        }

        // Login OK
        return ResponseEntity.ok(resp);
    }

    // ============================================================
    // ELENCO UTENTI (per condivisione note)
    // ============================================================
    @GetMapping
    public Collection<String> getAllUsernames() {
        return userService.getAllUsernames();
    }
}