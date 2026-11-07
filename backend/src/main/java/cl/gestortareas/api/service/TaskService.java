package cl.gestortareas.api.service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.model.Task;
import cl.gestortareas.api.dto.TaskDetailsRequest;
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
        task.setCompleted(task.isCompleted());
        return taskRepository.save(task);
    }

    @Transactional
    public Task update(Long id, Task updatedTask, String email) {
        Task existingTask = lockTask(id, email);

        existingTask.setDescription(
            updatedTask.getDescription()
        );

        existingTask.setCompleted(
            updatedTask.isCompleted()
        );

        return taskRepository.save(existingTask);
    }

    @Transactional
    public Task updateDetails(Long id, TaskDetailsRequest request, String email) {
        Task task = lockTask(id, email);
        if (request.dueDate() != null && (request.dueDate().getYear() < 1 || request.dueDate().getYear() > 9999)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha límite no es válida");
        }
        var tags = new LinkedHashMap<String, String>();
        for (String tag : request.tags()) {
            String normalized = tag.strip();
            tags.putIfAbsent(normalized.toLowerCase(Locale.ROOT), normalized);
        }
        task.updateDetails(request.description().strip(), request.priority(), request.status(),
            request.dueDate(), List.copyOf(tags.values()));
        return task;
    }

    private Task lockTask(Long id, String email) {
        return taskRepository.findForUpdateByIdAndOwnerEmail(id, email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada"));
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
