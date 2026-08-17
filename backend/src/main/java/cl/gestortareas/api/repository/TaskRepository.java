package cl.gestortareas.api.repository;

import cl.gestortareas.api.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
}