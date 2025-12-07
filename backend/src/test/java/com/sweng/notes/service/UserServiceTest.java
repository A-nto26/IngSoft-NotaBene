package com.sweng.notes.service;

import com.sweng.notes.dto.UserResponse;
import com.sweng.notes.model.Utente;
import com.sweng.notes.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

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
    // REGISTER — Sprint 4 (ritorna UserResponse, non boolean)
    // ============================================================
    @Test
    void testRegisterSuccess() {

        when(userRepo.exists("mario")).thenReturn(false);
        when(encoder.encode("12345678")).thenReturn("HASHED");

        UserResponse res = userService.register("mario", "12345678");

        assertTrue(res.isSuccess());
        assertEquals("mario", res.getUsername());
        verify(userRepo).save(any(Utente.class));
    }

    @Test
    void testRegisterFailUserExists() {

        when(userRepo.exists("mario")).thenReturn(true);

        assertThrows(ResponseStatusException.class,
                () -> userService.register("mario", "12345678"));

        verify(userRepo, never()).save(any());
    }

    @Test
    void testRegisterFailInvalidUsername() {
        assertThrows(ResponseStatusException.class,
                () -> userService.register("", "12345678"));

        assertThrows(ResponseStatusException.class,
                () -> userService.register(null, "12345678"));
    }

    @Test
    void testRegisterFailInvalidPassword() {
        assertThrows(ResponseStatusException.class,
                () -> userService.register("mario", ""));

        assertThrows(ResponseStatusException.class,
                () -> userService.register("mario", "short")); // < 8 chars
    }

    // ============================================================
    // LOGIN — Sprint 4 (ritorna UserResponse, non boolean)
    // ============================================================
    @Test
    void testLoginSuccess() {

        Utente u = new Utente("anna", "HASHED");
        when(userRepo.findByUsername("anna")).thenReturn(u);
        when(encoder.matches("pass1234", "HASHED")).thenReturn(true);

        UserResponse res = userService.login("anna", "pass1234");

        assertTrue(res.isSuccess());
        assertEquals("anna", res.getUsername());
    }

    @Test
    void testLoginFailWrongPassword() {

        Utente u = new Utente("anna", "HASHED");
        when(userRepo.findByUsername("anna")).thenReturn(u);
        when(encoder.matches("wrong", "HASHED")).thenReturn(false);

        UserResponse res = userService.login("anna", "wrong");

        assertFalse(res.isSuccess());
        assertEquals("Password errata", res.getMessage());
    }

    @Test
    void testLoginFailUserNotFound() {

        when(userRepo.findByUsername("xxx")).thenReturn(null);

        UserResponse res = userService.login("xxx", "pass");

        assertFalse(res.isSuccess());
        assertEquals("Utente non registrato", res.getMessage());
    }
}