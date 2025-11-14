package com.sweng.notes.controller;

import com.sweng.notes.service.UserService;
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

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestParam String username, @RequestParam String password) {
        boolean ok = userService.register(username, password);
        return ok ? ResponseEntity.ok("Registrazione completata")
                  : ResponseEntity.badRequest().body("Registrazione fallita");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String username, @RequestParam String password) {
        boolean ok = userService.login(username, password);
        return ok ? ResponseEntity.ok("Login corretto")
                  : ResponseEntity.badRequest().body("Credenziali invalide");
    }
}
