package com.taskmanagement.userservice.repository;

import com.taskmanagement.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link User}.
 * No custom queries are needed for the simple CRUD operations required here.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}
