# Objetivos compartidos

Desde un grupo, entra en **Objetivos**. El propietario, los administradores y los moderadores pueden crear, editar y eliminar metas. Todos los miembros pueden consultarlas.

Cada objetivo tiene un título, descripción opcional, fecha límite opcional y una selección de tareas existentes del mismo grupo. Las tareas se crean en **Progreso** y pueden contribuir a varios objetivos.

El avance es la cantidad de tareas completadas dividida por las tareas vinculadas, expresada como porcentaje redondeado. Sin tareas, el avance es cero. Cambiar el estado de una tarea actualiza todos sus objetivos al consultar sus datos. El botón **Actualizar** permite consultar cambios de otros usuarios.

Desde cada objetivo puedes abrir la tarea original o cambiar su estado si eres responsable de esa tarea o tienes permisos de administración. Eliminar un objetivo conserva las tareas. Si se elimina una tarea, deja de contar en los objetivos que la utilizaban.

## Ubicación del código

- `src/app/components/community-goals`: interfaz, formulario y pruebas de interacción.
- `src/app/services/community-goals.ts`: comunicación con la API.
- `src/app/models/community-goal.model.ts`: contratos de objetivos y tareas del grupo.
- `backend/src/main/java/cl/gestortareas/api/controller/CommunityGoalController.java`: endpoints de consulta, creación, edición y eliminación.
- `backend/src/main/java/cl/gestortareas/api/service/CommunityGoalService.java`: permisos, validación de tareas y cálculo de progreso.
- `backend/src/main/resources/db/migration/V53__create_community_goals.sql`: objetivos y vínculos con tareas, con restricciones para impedir vínculos entre grupos distintos.

La API usa `/api/communities/{groupId}/goals` para GET y POST, y `/{goalId}` para PUT y DELETE. Cada respuesta incluye los objetivos, las tareas disponibles y el permiso para administrar. Los estados se modifican mediante el endpoint existente de tareas del grupo.

No hay actualización en tiempo real por WebSocket. La vista se actualiza después de guardar y al pulsar **Actualizar** o volver a abrir la sección.
