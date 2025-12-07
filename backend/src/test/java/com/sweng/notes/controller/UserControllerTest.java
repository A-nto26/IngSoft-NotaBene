package com.sweng.notes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sweng.notes.dto.UserRequest;
import com.sweng.notes.dto.UserResponse;
import com.sweng.notes.service.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private final ObjectMapper mapper = new ObjectMapper();

    // ============================================================
    // REGISTER - SUCCESSO
    // ============================================================
    @Test
    void testRegisterSuccess() throws Exception {

        UserRequest req = new UserRequest("mario", "password123");

        when(userService.register("mario", "password123"))
                .thenReturn(new UserResponse(true, "Registrazione completata", "mario"));

        mockMvc.perform(
                post("/api/users/register")
                    .contentType("application/json")
                    .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.username").value("mario"));
    }

    // ============================================================
    // REGISTER - FALLIMENTO (es. utente già registrato)
    // ============================================================
    @Test
    void testRegisterFail() throws Exception {

        UserRequest req = new UserRequest("mario", "password123");

        when(userService.register("mario", "password123"))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Nome utente già registrato"));

        mockMvc.perform(
                post("/api/users/register")
                    .contentType("application/json")
                    .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }


    // ============================================================
    // LOGIN - SUCCESSO
    // ============================================================
    @Test
    void testLoginSuccess() throws Exception {

        UserRequest req = new UserRequest("anna", "pass");

        when(userService.login("anna", "pass"))
                .thenReturn(new UserResponse(true, "Login effettuato", "anna"));

        mockMvc.perform(
                post("/api/users/login")
                    .contentType("application/json")
                    .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.username").value("anna"));
    }

    // ============================================================
    // LOGIN - UTENTE NON REGISTRATO (404)
    // ============================================================
    @Test
    void testLoginUserNotFound() throws Exception {

        UserRequest req = new UserRequest("anna", "xxx");

        when(userService.login("anna", "xxx"))
                .thenReturn(new UserResponse(false, "Utente non registrato", null));

        mockMvc.perform(
                post("/api/users/login")
                    .contentType("application/json")
                    .content(mapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ============================================================
    // LOGIN - PASSWORD ERRATA (401)
    // ============================================================
    @Test
    void testLoginWrongPassword() throws Exception {

        UserRequest req = new UserRequest("anna", "sbagliata");

        when(userService.login("anna", "sbagliata"))
                .thenReturn(new UserResponse(false, "Password errata", null));

        mockMvc.perform(
                post("/api/users/login")
                    .contentType("application/json")
                    .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}