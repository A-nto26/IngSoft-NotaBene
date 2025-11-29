package com.sweng.notes.service;

import com.sweng.notes.model.Utente;
import com.sweng.notes.repository.UserRepository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    @Autowired
    public UserService(UserRepository userRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.encoder = encoder;
    }

    // ============================================================
    // GET ALL USERS (per la condivisione note)
    // ============================================================
    public List<String> getAllUsernames() {
        return userRepo.findAll()
                .stream()
                .map(u -> u.getUsername().trim().toLowerCase())
                .toList();
    }

    // ============================================================
    // REGISTER
    // ============================================================
    public boolean register(String username, String password) {

        if (username == null || username.isBlank() ||
                password == null || password.isBlank()) {
            return false;
        }

        username = username.trim().toLowerCase();

        if (userRepo.exists(username))
            return false;

        String hash = encoder.encode(password);
        Utente nuovo = new Utente(username, hash);
        userRepo.save(nuovo);

        return true;
    }

    // ============================================================
    // LOGIN
    // ============================================================
    public boolean login(String username, String password) {

        if (username == null || password == null)
            return false;

        username = username.trim().toLowerCase();

        Utente u = userRepo.findByUsername(username);
        if (u == null)
            return false;

        return encoder.matches(password, u.getPasswordHash());
    }
}
