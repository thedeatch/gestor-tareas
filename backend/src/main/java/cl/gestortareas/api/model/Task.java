package cl.gestortareas.api.model;

import java.time.LocalDate;
import java.util.List;

import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(
    name = "Task",
    description = "Tarea administrada por la aplicación"
)
@Entity
@DynamicUpdate
@Table(name = "tasks")
public class Task {
    public enum Priority { LOW, NORMAL, HIGH }
    public enum Status { PENDING, IN_PROGRESS, COMPLETED }

	@Schema(
		description = "Identificador único generado por el servidor",
		example = "1",
		accessMode = Schema.AccessMode.READ_ONLY
	)
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Schema(
		description = "Descripción de la tarea",
		example = "Documentar la API con Swagger",
		maxLength = 150
	)
	@NotBlank(message = "La descripción es obligatoria")
	@Size(
		max = 150,
		message = "La descripción no puede superar los 150 caracteres"
	)
	private String description;
	@Schema(
		description = "Indica si la tarea está completada",
		example = "false"
	)
	private boolean completed;

	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private AppUser owner;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "category_id")
    private Long categoryId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Long getProjectId() { return projectId; }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Long getCategoryId() { return categoryId; }

    public void classify(Long projectId, Long categoryId) {
        this.projectId = projectId;
        this.categoryId = categoryId;
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority = Priority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Status status = Status.PENDING;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "tags_text", nullable = false, length = 310)
    private String tagsText = "";

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Priority getPriority() { return priority; }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Status getStatus() { return status; }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public LocalDate getDueDate() { return dueDate; }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public List<String> getTags() {
        return tagsText.isEmpty() ? List.of() : List.of(tagsText.split("\\n"));
    }

    public void updateDetails(String description, Priority priority, Status status, LocalDate dueDate,
                              List<String> tags) {
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.completed = status == Status.COMPLETED;
        this.dueDate = dueDate;
        this.tagsText = String.join("\n", tags);
    }

    public Task() {
    }

    public Task(String description, boolean completed) {
        this.description = description;
        setCompleted(completed);
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        if (completed) {
            status = Status.COMPLETED;
        } else if (status == Status.COMPLETED) {
            status = Status.PENDING;
        }
    }

    public AppUser getOwner() {
        return owner;
    }

    public void setOwner(AppUser owner) {
        this.owner = owner;
    }
}
