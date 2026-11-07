package cl.gestortareas.api.model;

import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Entity;
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

    public Task() {
    }

    public Task(String description, boolean completed) {
        this.description = description;
        this.completed = completed;
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
    }

    public AppUser getOwner() {
        return owner;
    }

    public void setOwner(AppUser owner) {
        this.owner = owner;
    }
}
