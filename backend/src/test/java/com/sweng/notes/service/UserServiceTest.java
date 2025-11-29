package com.sweng.notes.service;

import com.sweng.notes.model.Utente;
import com.sweng.notes.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    private UserRepository userRepo;
    private PasswordEncoder encoder;
    private UserService userService;

    @BeforeEach
    void setup() {
        userRepo = mock(UserRepository.class);
        encoder = mock(PasswordEncoder.class);

        userService = new UserService(userRepo, encoder);
    }

    // ============================================================
    // REGISTER
    // ============================================================
    @Test
    void testRegisterSuccess() {
        when(userRepo.exists("mario")).thenReturn(false);
        when(encoder.encode("1234")).thenReturn("HASHED1234");

        boolean result = userService.register("mario", "1234");

        assertTrue(result);
        verify(userRepo).save(any(Utente.class));
    }

    @Test
    void testRegisterFailsIfUserExists() {
        when(userRepo.exists("mario")).thenReturn(true);

        assertFalse(userService.register("mario", "1234"));
        verify(userRepo, never()).save(any());
    }

    @Test
    void testRegisterFailsOnInvalidInput() {
        assertFalse(userService.register("", "123"));
        assertFalse(userService.register("mario", ""));
        assertFalse(userService.register(null, "123"));
        assertFalse(userService.register("mario", null));
    }

    // ============================================================
    // LOGIN
    // ============================================================
    @Test
    void testLoginSuccess() {
        Utente u = new Utente("anna", "HASHEDPASS");

        when(userRepo.findByUsername("anna")).thenReturn(u);
        when(encoder.matches("pass", "HASHEDPASS")).thenReturn(true);

        assertTrue(userService.login("anna", "pass"));
    }

    @Test
    void testLoginFailsWrongPassword() {
        Utente u = new Utente("anna", "HASHEDPASS");

        when(userRepo.findByUsername("anna")).thenReturn(u);
        when(encoder.matches("xxx", "HASHEDPASS")).thenReturn(false);

        assertFalse(userService.login("anna", "xxx"));
    }

    @Test
    void testLoginFailsUserNotFound() {
        when(userRepo.findByUsername("xxx")).thenReturn(null);

        assertFalse(userService.login("xxx", "123"));
    }
}
