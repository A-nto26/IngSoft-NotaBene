package com.sweng.notes.service;

import com.sweng.notes.model.Utente;
import com.sweng.notes.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    private UserRepository userRepo;
    private UserService userService;

    @BeforeEach
    void setup() {
        userRepo = Mockito.mock(UserRepository.class);
        userService = new UserService(userRepo);
    }

    @Test
    void testRegisterSuccess() {
        when(userRepo.exists("mario")).thenReturn(false);

        boolean result = userService.register("mario", "1234");

        assertTrue(result);
        verify(userRepo, times(1)).save(any(Utente.class));
    }

    @Test
    void testRegisterFailsIfUserExists() {
        when(userRepo.exists("mario")).thenReturn(true);

        boolean result = userService.register("mario", "1234");

        assertFalse(result);
        verify(userRepo, never()).save(any());
    }

    @Test
    void testRegisterFailsOnEmptyFields() {
        assertFalse(userService.register("", "123"));
        assertFalse(userService.register("mario", ""));
        assertFalse(userService.register(null, "123"));
        assertFalse(userService.register("mario", null));
    }

    @Test
    void testLoginSuccess() {
        Utente u = new Utente("anna", "pass");
        when(userRepo.find("anna")).thenReturn(u);

        assertTrue(userService.login("anna", "pass"));
    }

    @Test
    void testLoginFailsWrongPassword() {
        when(userRepo.find("anna")).thenReturn(new Utente("anna", "pass"));

        assertFalse(userService.login("anna", "xxx"));
    }

    @Test
    void testLoginFailsUserNotFound() {
        when(userRepo.find("xxx")).thenReturn(null);

        assertFalse(userService.login("xxx", "123"));
    }
}
