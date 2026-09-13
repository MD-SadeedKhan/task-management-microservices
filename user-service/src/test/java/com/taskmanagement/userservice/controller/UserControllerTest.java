package com.taskmanagement.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.userservice.dto.UserRequest;
import com.taskmanagement.userservice.dto.UserResponse;
import com.taskmanagement.userservice.exception.ResourceNotFoundException;
import com.taskmanagement.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void createUser_returns201() throws Exception {
        UserResponse response = new UserResponse(1L, "Ada Lovelace", "ada@example.com");
        when(userService.createUser(any(UserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRequest("Ada Lovelace", "ada@example.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ada@example.com"));
    }

    @Test
    void createUser_withInvalidPayload_returns400() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRequest("", "not-an-email"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllUsers_returns200WithList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(new UserResponse(1L, "Ada Lovelace", "ada@example.com")));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ada Lovelace"));
    }

    @Test
    void getUserById_whenExists_returns200() throws Exception {
        when(userService.getUserById(eq(1L))).thenReturn(new UserResponse(1L, "Ada Lovelace", "ada@example.com"));

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getUserById_whenMissing_returns404() throws Exception {
        when(userService.getUserById(eq(404L))).thenThrow(new ResourceNotFoundException("User not found with id: 404"));

        mockMvc.perform(get("/users/404"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_returns204() throws Exception {
        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isNoContent());
    }
}
