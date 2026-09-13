package com.taskmanagement.taskservice.service;

import com.taskmanagement.taskservice.dto.TaskRequest;
import com.taskmanagement.taskservice.dto.TaskResponse;
import com.taskmanagement.taskservice.entity.Task;
import com.taskmanagement.taskservice.exception.ResourceNotFoundException;
import com.taskmanagement.taskservice.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Business logic for task management.
 * Uses plain constructor injection - no Lombok, no field injection.
 */
@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public TaskResponse createTask(TaskRequest request) {
        Task task = new Task(request.getTitle(), request.getDescription(), request.getStatus(), request.getUserId());
        Task saved = taskRepository.save(task);
        return toResponse(saved);
    }

    public List<TaskResponse> getAllTasks() {
        return taskRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return toResponse(task);
    }

    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Task not found with id: " + id);
        }
        taskRepository.deleteById(id);
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(task.getId(), task.getTitle(), task.getDescription(), task.getStatus(), task.getUserId());
    }
}
