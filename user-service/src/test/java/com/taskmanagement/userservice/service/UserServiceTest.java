package com.taskmanagement.userservice.service;

import com.taskmanagement.userservice.dto.UserRequest;
import com.taskmanagement.userservice.dto.UserResponse;
import com.taskmanagement.userservice.entity.User;
import com.taskmanagement.userservice.exception.ResourceNotFoundException;
import com.taskmanagement.userservice.repository.UserRepository;
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
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void createUser_savesAndReturnsUser() {
        UserRequest request = new UserRequest("Ada Lovelace", "ada@example.com");
        User saved = new User("Ada Lovelace", "ada@example.com");
        saved.setId(1L);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserResponse response = userService.createUser(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Ada Lovelace");
        assertThat(response.getEmail()).isEqualTo("ada@example.com");
    }

    @Test
    void getAllUsers_returnsMappedList() {
        User user = new User("Grace Hopper", "grace@example.com");
        user.setId(2L);
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserResponse> result = userService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("grace@example.com");
    }

    @Test
    void getUserById_whenFound_returnsUser() {
        User user = new User("Alan Turing", "alan@example.com");
        user.setId(3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(3L);

        assertThat(response.getName()).isEqualTo("Alan Turing");
    }

    @Test
    void getUserById_whenNotFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void deleteUser_whenExists_deletes() {
        when(userRepository.existsById(5L)).thenReturn(true);

        userService.deleteUser(5L);

        verify(userRepository, times(1)).deleteById(5L);
    }

    @Test
    void deleteUser_whenNotFound_throwsException() {
        when(userRepository.existsById(anyLong())).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(100L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
