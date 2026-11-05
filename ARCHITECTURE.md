# Arquitectura

## Tipo de aplicación

GestorTareas es un **monolito modular full stack**. No está dividido en microservicios: existe una API Spring Boot desplegable como una sola unidad, organizada internamente por controladores, servicios, repositorios y modelos.

```text
Navegador
   │ HTTPS / JSON
   ▼
Angular ─────► Spring Boot API ─────► PostgreSQL
                  │      │
                  │      └──────────► Proveedor SMTP
                  └─────────────────► Almacenamiento de archivos en BD
```

## Componentes

### Frontend

- Aplicación Angular standalone.
- Componentes por funcionalidad y servicios para acceder a la API.
- Guards y permisos para proteger navegación.
- Tema claro/oscuro y reducción de movimiento por usuario.
- Nginx sirve la compilación de producción dentro de Docker.

### Backend

- Controladores REST reciben y validan solicitudes.
- Servicios contienen las reglas de negocio.
- Repositorios Spring Data acceden a PostgreSQL.
- Un manejador global entrega errores con un formato uniforme.
- Swagger/OpenAPI documenta los contratos HTTP.

### Datos

- PostgreSQL almacena usuarios, tareas, sesiones, archivos y auditoría.
- Flyway versiona el esquema; no se modifica con `ddl-auto=update`.
- Los archivos reutilizables guardan metadatos, contenido y propietario.

## Seguridad

1. La contraseña se almacena como hash BCrypt, nunca en texto plano.
2. El inicio de sesión entrega un JWT breve y un refresh token renovable.
3. Los refresh tokens permiten listar y cerrar sesiones por dispositivo.
4. RBAC distingue `USER`, `MODERATOR` y `ADMIN`.
5. El backend vuelve a comprobar el rol y el estado activo de la cuenta.
6. Recuperación y verificación usan tokens temporales enviados por SMTP.

## Flujo de una solicitud protegida

1. Angular envía `Authorization: Bearer <token>`.
2. Spring Security valida firma, emisor, vigencia, rol y estado de cuenta.
3. El controlador delega la operación al servicio.
4. El servicio aplica permisos y reglas de propiedad.
5. El repositorio consulta o modifica PostgreSQL.
6. La API responde JSON o un error uniforme.

## Ejecución y despliegue

- Docker Compose levanta frontend, backend y PostgreSQL localmente.
- GitHub Actions prueba y compila ambos proyectos, luego valida sus imágenes.
- Render crea la base de datos, la API Docker y el sitio estático.
- `/api/health` es la comprobación pública y liviana de disponibilidad.

## Decisiones para reutilización

El núcleo transversal debe evitar depender del módulo de tareas. Autenticación, usuarios, permisos, preferencias, sesiones, archivos, errores y auditoría pueden reutilizarse. Los módulos funcionales nuevos deben seguir la guía de [MODULE-GUIDE.md](MODULE-GUIDE.md).
