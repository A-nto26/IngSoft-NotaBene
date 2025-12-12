package com.sweng.notes.controller;

import com.sweng.notes.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.sweng.notes.dto.UserResponse;
import com.sweng.notes.dto.UserRequest;

import java.util.Collection;

/** * Controller REST per la gestione degli utenti.
 *  Funzionalità:
 *  - Registrazione (username + password) 
 *  - Login 
 *  - Elenco username (per condivisione note) 
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

    private String normalize(String s) {
        return (s == null ? null : s.trim().toLowerCase());
    }

    // ============================================================
    // POST - REGISTRAZIONE
    // ============================================================
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody UserRequest req) {

        String username = normalize(req.getUsername());
        String password = req.getPassword();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new UserResponse(false, "Username o password mancanti", null));
        }

        UserResponse response = userService.register(username, password);

        if (!response.isSuccess()) {
            // Utente già registrato
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // POST - LOGIN
    // ============================================================
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@RequestBody UserRequest req) {

        String username = normalize(req.getUsername());
        String password = req.getPassword();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new UserResponse(false, "Username o password mancanti", null));
        }

        UserResponse resp = userService.login(username, password);

        if (!resp.isSuccess()) {

            return switch (resp.getMessage()) {
                case "Utente non registrato" ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
                case "Password errata" ->
                        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
                default ->
                        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
            };
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
