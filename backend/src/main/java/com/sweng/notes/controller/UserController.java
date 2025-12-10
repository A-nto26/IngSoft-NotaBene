package com.sweng.notes.controller;

import com.sweng.notes.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.sweng.notes.dto.UserResponse;
import com.sweng.notes.dto.UserRequest;


import java.util.Collection;

/**
 * Controller REST per la gestione degli utenti.
 * Funzionalità:
 * - Registrazione (username + password)
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

    // ============================================================
    // POST - REGISTRAZIONE
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
    // POST - LOGIN
    // ============================================================
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@RequestBody UserRequest req) {

        UserResponse resp = userService.login(req.getUsername(), req.getPassword());

        if (!resp.isSuccess()) {

            if ("Utente non registrato".equals(resp.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
            }

            if ("Password errata".equals(resp.getMessage())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
        }

        return ResponseEntity.ok(resp);
    }

    // ============================================================
    // GET - ELENCO UTENTI (per condivisione note)
    // ============================================================
    @GetMapping
    public Collection<String> getAllUsernames() {
        return userService.getAllUsernames();
    }
}