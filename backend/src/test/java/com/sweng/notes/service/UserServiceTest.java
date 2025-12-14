package com.sweng.notes.service;

import com.sweng.notes.dto.UserResponse;
import com.sweng.notes.model.Utente;
import com.sweng.notes.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepo;
    private PasswordEncoder encoder;
    private UserService userService;

    @BeforeEach
    void setup() {
        userRepo = mock(UserRepository.class);
        encoder = mock(PasswordEncoder.class);
        userService = new UserService(userRepo, encoder);
    }

    @Test
    void testRegister_ok() {
        when(userRepo.exists("mario")).thenReturn(false);
        when(encoder.encode("password123")).thenReturn("HASHED");

        UserResponse res = userService.register("mario", "password123");

        assertTrue(res.isSuccess());
        assertEquals("mario", res.getUsername());
        verify(userRepo).save(any(Utente.class));
    }

    @Test
    void testRegister_fail_userExists() {
        when(userRepo.exists("mario")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.register("mario", "password123"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(userRepo, never()).save(any());
    }

    @Test
    void testLogin_ok() {
        Utente u = new Utente("anna", "HASHED");
        when(userRepo.findByUsername("anna")).thenReturn(u);
        when(encoder.matches("password123", "HASHED")).thenReturn(true);

        UserResponse res = userService.login("anna", "password123");

        assertTrue(res.isSuccess());
        assertEquals("anna", res.getUsername());
    }

    @Test
    void testLogin_fail_wrongPassword() {
        Utente u = new Utente("anna", "HASHED");
        when(userRepo.findByUsername("anna")).thenReturn(u);
        when(encoder.matches("wrong", "HASHED")).thenReturn(false);

        UserResponse res = userService.login("anna", "wrong");

        assertFalse(res.isSuccess());
        assertEquals("Password errata", res.getMessage());
    }
}
