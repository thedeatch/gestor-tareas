package cl.gestortareas.api.dto;

import java.time.LocalDate;
import java.util.List;

import cl.gestortareas.api.model.Task.Priority;
import cl.gestortareas.api.model.Task.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TaskDetailsRequest(
    @NotBlank @Size(max = 150) String description,
    @NotNull Priority priority,
    @NotNull Status status,
    LocalDate dueDate,
    @NotNull @Size(max = 10) List<
        @NotBlank @Size(max = 30) @Pattern(regexp = "[^,\\p{Cntrl}]+") String> tags
) {}