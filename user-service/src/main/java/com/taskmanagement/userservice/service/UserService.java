package com.taskmanagement.userservice.service;

import com.taskmanagement.userservice.dto.UserRequest;
import com.taskmanagement.userservice.dto.UserResponse;
import com.taskmanagement.userservice.entity.User;
import com.taskmanagement.userservice.exception.ResourceNotFoundException;
import com.taskmanagement.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Business logic for user management.
 * Uses plain constructor injection - no Lombok, no field injection.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse createUser(UserRequest request) {
        User user = new User(request.getName(), request.getEmail());
        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return toResponse(user);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
