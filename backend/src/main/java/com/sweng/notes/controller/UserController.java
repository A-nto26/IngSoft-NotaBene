package com.sweng.notes.controller;

import com.sweng.notes.dto.UserRequest;
import com.sweng.notes.dto.UserResponse;
import com.sweng.notes.service.UserService;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ============================================================
    // GET ALL USERS (per la condivisione note)
    // ============================================================
    @GetMapping
    public ResponseEntity<List<String>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsernames());
    }
    
    // ============================================================
    // REGISTER
    // ============================================================
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody UserRequest req) {

        boolean ok = userService.register(req.getUsername(), req.getPassword());

        if (!ok) {
            return ResponseEntity.badRequest()
                    .body(new UserResponse(false, "❌ Registrazione fallita (utente esistente o dati invalidi)"));
        }

        return ResponseEntity.ok(
                new UserResponse(true, "✔ Registrazione completata", req.getUsername())
        );
    }

    // ============================================================
    // LOGIN
    // ============================================================
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@RequestBody UserRequest req) {

        boolean ok = userService.login(req.getUsername(), req.getPassword());

        if (!ok) {
            return ResponseEntity.badRequest()
                    .body(new UserResponse(false, "❌ Credenziali non valide"));
        }

        return ResponseEntity.ok(
                new UserResponse(true, "✔ Login effettuato", req.getUsername())
        );
    }
}
