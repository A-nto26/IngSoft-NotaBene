package com.sweng.notes.service;

import com.sweng.notes.model.Utente;
import com.sweng.notes.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepo;

    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public boolean register(String username, String password) {
        if (username == null || username.isBlank() ||
                password == null || password.isBlank()) {
            return false;
        }

        if (userRepo.exists(username))
            return false;

        userRepo.save(new Utente(username, password));
        return true;
    }

    public boolean login(String username, String password) {
        Utente u = userRepo.find(username);
        return u != null && u.getPassword().equals(password);
    }
}
