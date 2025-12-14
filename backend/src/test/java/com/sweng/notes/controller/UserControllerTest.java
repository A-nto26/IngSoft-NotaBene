package com.sweng.notes.controller;

import com.sweng.notes.dto.UserRequest;
import com.sweng.notes.dto.UserResponse;
import com.sweng.notes.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserControllerTest {

    private UserService userService;
    private UserController controller;

    @BeforeEach
    void setup() {
        userService = mock(UserService.class);
        controller = new UserController(userService);
    }

    @Test
    void testRegister_ok() {
        when(userService.register("mario", "password123"))
                .thenReturn(new UserResponse(true, "Registrazione completata", "mario"));

        ResponseEntity<UserResponse> res =
                controller.register(new UserRequest("mario", "password123"));

        assertEquals(200, res.getStatusCode().value());
        assertNotNull(res.getBody());
        assertTrue(res.getBody().isSuccess());

        verify(userService).register("mario", "password123");
    }

    @Test
    void testRegister_badRequest_missingFields() {
        ResponseEntity<UserResponse> res =
                controller.register(new UserRequest("", ""));

        assertEquals(400, res.getStatusCode().value());
        verifyNoInteractions(userService);
    }

    @Test
    void testLogin_ok() {
        when(userService.login("anna", "password123"))
                .thenReturn(new UserResponse(true, "Login effettuato", "anna"));

        ResponseEntity<UserResponse> res =
                controller.login(new UserRequest("anna", "password123"));

        assertEquals(200, res.getStatusCode().value());
        assertNotNull(res.getBody());
        assertTrue(res.getBody().isSuccess());

        verify(userService).login("anna", "password123");
    }

    @Test
    void testGetAllUsernames_ok() {
        when(userService.getAllUsernames()).thenReturn(List.of("anna", "mario"));

        assertEquals(2, controller.getAllUsernames().size());
        verify(userService).getAllUsernames();
    }
}
