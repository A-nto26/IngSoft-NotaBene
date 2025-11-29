package com.sweng.notes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sweng.notes.dto.UserRequest;
import com.sweng.notes.service.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testRegisterSuccess() throws Exception {

        UserRequest req = new UserRequest("mario", "1234");

        when(userService.register("mario", "1234")).thenReturn(true);

        mockMvc.perform(
                post("/api/users/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(req))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testRegisterFail() throws Exception {

        UserRequest req = new UserRequest("mario", "1234");

        when(userService.register("mario", "1234")).thenReturn(false);

        mockMvc.perform(
                post("/api/users/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(req))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testLoginSuccess() throws Exception {

        UserRequest req = new UserRequest("anna", "pass");

        when(userService.login("anna", "pass")).thenReturn(true);

        mockMvc.perform(
                post("/api/users/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(req))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testLoginFail() throws Exception {

        UserRequest req = new UserRequest("anna", "xxx");

        when(userService.login("anna", "xxx")).thenReturn(false);

        mockMvc.perform(
                post("/api/users/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(req))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }
}
