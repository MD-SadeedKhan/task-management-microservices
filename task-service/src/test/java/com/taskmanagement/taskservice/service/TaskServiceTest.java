package com.taskmanagement.taskservice.service;

import com.taskmanagement.taskservice.dto.TaskRequest;
import com.taskmanagement.taskservice.dto.TaskResponse;
import com.taskmanagement.taskservice.entity.Task;
import com.taskmanagement.taskservice.entity.TaskStatus;
import com.taskmanagement.taskservice.exception.ResourceNotFoundException;
import com.taskmanagement.taskservice.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository);
    }

    @Test
    void createTask_savesAndReturnsTask() {
        TaskRequest request = new TaskRequest("Write report", "Quarterly report", TaskStatus.TODO, 1L);
        Task saved = new Task("Write report", "Quarterly report", TaskStatus.TODO, 1L);
        saved.setId(1L);
        when(taskRepository.save(any(Task.class))).thenReturn(saved);

        TaskResponse response = taskService.createTask(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Write report");
        assertThat(response.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(response.getUserId()).isEqualTo(1L);
    }

    @Test
    void getAllTasks_returnsMappedList() {
        Task task = new Task("Fix bug", "Null pointer", TaskStatus.IN_PROGRESS, 2L);
        task.setId(2L);
        when(taskRepository.findAll()).thenReturn(List.of(task));

        List<TaskResponse> result = taskService.getAllTasks();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void getTaskById_whenFound_returnsTask() {
        Task task = new Task("Deploy service", "Prod release", TaskStatus.DONE, 3L);
        task.setId(3L);
        when(taskRepository.findById(3L)).thenReturn(Optional.of(task));

        TaskResponse response = taskService.getTaskById(3L);

        assertThat(response.getTitle()).isEqualTo("Deploy service");
    }

    @Test
    void getTaskById_whenNotFound_throwsException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void deleteTask_whenExists_deletes() {
        when(taskRepository.existsById(5L)).thenReturn(true);

        taskService.deleteTask(5L);

        verify(taskRepository, times(1)).deleteById(5L);
    }

    @Test
    void deleteTask_whenNotFound_throwsException() {
        when(taskRepository.existsById(anyLong())).thenReturn(false);

        assertThatThrownBy(() -> taskService.deleteTask(100L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
