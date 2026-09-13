package com.taskmanagement.taskservice.repository;

import com.taskmanagement.taskservice.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Task}.
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
}
