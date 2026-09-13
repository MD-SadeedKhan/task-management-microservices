package com.taskmanagement.taskservice.dto;

import com.taskmanagement.taskservice.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload accepted by {@code POST /tasks}.
 */
public class TaskRequest {

    @NotBlank(message = "title must not be blank")
    private String title;

    private String description;

    @NotNull(message = "status must not be null")
    private TaskStatus status;

    @NotNull(message = "userId must not be null")
    private Long userId;

    public TaskRequest() {
    }

    public TaskRequest(String title, String description, TaskStatus status, Long userId) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
