package com.sweng.notes.controller;

import com.sweng.notes.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserControllerTest {

    private UserService userService;
    private UserController controller;

    @BeforeEach
    void setup() {
        userService = Mockito.mock(UserService.class);
        controller = new UserController(userService);
    }

    @Test
    void testRegisterSuccess() {
        when(userService.register("mario", "1234")).thenReturn(true);

        ResponseEntity<String> response =
                controller.register("mario", "1234");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Registrazione completata", response.getBody());
    }

    @Test
    void testRegisterFail() {
        when(userService.register("mario", "1234")).thenReturn(false);

        ResponseEntity<String> response =
                controller.register("mario", "1234");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Registrazione fallita", response.getBody());
    }

    @Test
    void testLoginSuccess() {
        when(userService.login("anna", "pass")).thenReturn(true);

        ResponseEntity<String> response =
                controller.login("anna", "pass");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Login corretto", response.getBody());
    }

    @Test
    void testLoginFail() {
        when(userService.login("anna", "pass")).thenReturn(false);

        ResponseEntity<String> response =
                controller.login("anna", "pass");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Credenziali invalide", response.getBody());
    }
}
