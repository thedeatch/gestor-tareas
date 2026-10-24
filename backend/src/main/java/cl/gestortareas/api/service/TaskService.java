package cl.gestortareas.api.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.model.Task;
import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.repository.AppUserRepository;
import cl.gestortareas.api.repository.TaskRepository;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final AppUserRepository userRepository;

    public TaskService(
        TaskRepository taskRepository,
        AppUserRepository userRepository
    ) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    public List<Task> findAll(String email) {
        return taskRepository.findAllByOwnerEmailOrderByIdAsc(email);
    }

    public Task findById(Long id, String email) {
        return taskRepository.findByIdAndOwnerEmail(id, email)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Tarea no encontrada"
                )
            );
    }

    public Task create(Task task, String email) {
        AppUser owner = findUser(email);
        task.setOwner(owner);
        return taskRepository.save(task);
    }

    public Task update(Long id, Task updatedTask, String email) {
        Task existingTask = findById(id, email);

        existingTask.setDescription(
            updatedTask.getDescription()
        );

        existingTask.setCompleted(
            updatedTask.isCompleted()
        );

        return taskRepository.save(existingTask);
    }

    public void delete(Long id, String email) {
        Task existingTask = findById(id, email);
        taskRepository.delete(existingTask);
    }

    private AppUser findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Usuario autenticado no encontrado"
            ));
    }
}
