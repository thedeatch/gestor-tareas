package cl.gestortareas.api.repository;

import cl.gestortareas.api.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByOwnerEmailOrderByIdAsc(String email);

    Optional<Task> findByIdAndOwnerEmail(Long id, String email);
}
