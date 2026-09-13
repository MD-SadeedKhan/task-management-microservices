package com.taskmanagement.taskservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.taskservice.dto.TaskRequest;
import com.taskmanagement.taskservice.dto.TaskResponse;
import com.taskmanagement.taskservice.entity.TaskStatus;
import com.taskmanagement.taskservice.exception.ResourceNotFoundException;
import com.taskmanagement.taskservice.service.TaskService;
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

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @Test
    void createTask_returns201() throws Exception {
        TaskResponse response = new TaskResponse(1L, "Write report", "Quarterly report", TaskStatus.TODO, 1L);
        when(taskService.createTask(any(TaskRequest.class))).thenReturn(response);

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TaskRequest("Write report", "Quarterly report", TaskStatus.TODO, 1L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void createTask_withInvalidPayload_returns400() throws Exception {
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TaskRequest("", "Missing title and status", null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllTasks_returns200WithList() throws Exception {
        when(taskService.getAllTasks()).thenReturn(
                List.of(new TaskResponse(1L, "Write report", "Quarterly report", TaskStatus.TODO, 1L)));

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Write report"));
    }

    @Test
    void getTaskById_whenExists_returns200() throws Exception {
        when(taskService.getTaskById(eq(1L)))
                .thenReturn(new TaskResponse(1L, "Write report", "Quarterly report", TaskStatus.TODO, 1L));

        mockMvc.perform(get("/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getTaskById_whenMissing_returns404() throws Exception {
        when(taskService.getTaskById(eq(404L)))
                .thenThrow(new ResourceNotFoundException("Task not found with id: 404"));

        mockMvc.perform(get("/tasks/404"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTask_returns204() throws Exception {
        mockMvc.perform(delete("/tasks/1"))
                .andExpect(status().isNoContent());
    }
}
