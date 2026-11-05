# GestorTareas

Aplicación full stack para gestionar tareas y servir como base reutilizable de futuros proyectos. Incluye autenticación, perfiles, roles, administración, auditoría, archivos, preferencias y gestión de sesiones.

## Tecnologías

- Angular 22, TypeScript y Angular Material.
- Spring Boot 4 y Java 17.
- PostgreSQL 17 y Flyway.
- JWT, refresh tokens, BCrypt y RBAC.
- Docker Compose, GitHub Actions y Render.

## Arquitectura

El proyecto es un **monolito modular**, no una arquitectura de microservicios. Angular es el cliente web, Spring Boot expone la API REST y PostgreSQL conserva los datos. Consulta [ARCHITECTURE.md](ARCHITECTURE.md) para conocer el diseño y las responsabilidades.

## Inicio rápido con Docker

Requisitos: Git y Docker Desktop con el motor activo.

1. Copia `.env.example` como `.env`.
2. Sustituye las contraseñas y secretos de ejemplo.
3. Desde la raíz ejecuta:

```powershell
docker compose up -d --build
```

Servicios disponibles:

- Frontend: http://localhost:4200
- API: http://localhost:8081/api/health
- Swagger: http://localhost:8081/swagger-ui/index.html
- PostgreSQL: `localhost:5432`

Para detenerlos sin borrar los datos:

```powershell
docker compose down
```

## Desarrollo local

Primero inicia PostgreSQL:

```powershell
docker compose up -d postgres
```

En una terminal, inicia el backend:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

En otra terminal, inicia Angular:

```powershell
npm ci
npm start
```

El proxy de desarrollo dirige las solicitudes `/api` de Angular hacia Spring Boot.

## Variables de entorno

La referencia completa está en [.env.example](.env.example). Las variables más importantes son:

- `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`: conexión a PostgreSQL.
- `JWT_SECRET`: secreto de al menos 32 caracteres.
- `REFRESH_TOKEN_COOKIE_SECURE`: `false` en HTTP local y `true` en HTTPS.
- `MAIL_*`: credenciales SMTP para verificación y recuperación.
- `FRONTEND_URL`: URL usada en enlaces enviados por correo.
- `BOOTSTRAP_ADMIN_EMAIL`: correo de una cuenta ya registrada que se promoverá inicialmente.
- `EMAIL_VERIFICATION_ENABLED`: permite desactivar la verificación únicamente en desarrollo.

No publiques el archivo `.env` ni credenciales reales.

## Calidad y pruebas

Frontend:

```powershell
npm test -- --watch=false
npm run build
```

Backend:

```powershell
cd backend
.\mvnw.cmd clean verify
```

Imágenes Docker:

```powershell
docker build -t gestor-tareas-api:dev .\backend
docker build -t gestor-tareas-frontend:dev .
```

GitHub Actions ejecuta automáticamente estas validaciones en cada push o pull request dirigido a `Dev` o `main`. Render despliega solamente cuando los controles terminan correctamente.

## Documentación del proyecto

- [ARCHITECTURE.md](ARCHITECTURE.md): componentes, seguridad y flujo de datos.
- [MODULE-GUIDE.md](MODULE-GUIDE.md): guía para agregar un módulo reutilizable.
- [ROADMAP.md](ROADMAP.md): estado de la base transversal y próximos hitos.
- Swagger local: http://localhost:8081/swagger-ui/index.html

## Estructura

```text
gestor-tareas/
├── src/                 # Aplicación Angular
├── backend/             # API Spring Boot y migraciones Flyway
├── .github/workflows/   # Integración continua
├── compose.yaml         # Entorno local completo
├── render.yaml          # Infraestructura de despliegue
└── .env.example         # Plantilla de configuración
```

## Flujo de ramas

- `Dev`: integración y despliegue del entorno actual.
- `main`: versión estable.
- Los cambios se integran mediante commits convencionales, por ejemplo `feat:`, `fix:`, `test:` y `docs:`.
