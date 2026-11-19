# Actualización de dependencias — 19 de noviembre de 2026

Se completó la etapa de dependencias posterior a la revisión técnica.

## Cambios

- Angular, Angular CLI, build, compiladores, CDK y Material quedaron alineados en **22.3.0**. El manifiesto permite parches de esa línea mediante `~22.3.0`.
- Spring Boot pasó de **4.1.0 a 4.1.2**, con sus dependencias administradas.
- AWS S3 pasó de **2.57.11 a 2.57.13**. Springdoc conserva **3.1.1**.
- Se regeneró el archivo de dependencias de npm y se comprobó una instalación limpia mediante `npm ci`. También se actualizaron dependencias transitivas compatibles.
- Se añadió `scripts/audit-backend-dependencies.ps1` para consultar OSV usando las coordenadas públicas y las versiones del árbol Maven. No envía código de la aplicación ni credenciales.

No se usaron `--force` ni `--legacy-peer-deps`. El primer intento de resolución chocó con las versiones antiguas instaladas; se resolvió el manifiesto actualizado en un directorio temporal y se validó el archivo resultante con una instalación limpia.

## Resultados

| Comprobación | Resultado |
|---|---|
| npm audit, incluyendo desarrollo | **0 alertas**, frente a 41 antes de actualizar |
| Integridad de dependencias npm | `npm ls --depth=0` correcto; paquetes Angular alineados |
| Frontend | **106 pruebas correctas**, en 47 archivos |
| Build de producción Angular | Correcto, con advertencias de tamaño |
| Backend: Maven verify | **108 pruebas correctas** y ejecutable construido |
| OSV: JAR de dependencias Maven de ejecución | **157 coordenadas/versiones consultadas**, sin alertas devueltas |

Las consultas describen el estado de los registros consultados en esta fecha. La consulta de Maven cubre dependencias compile/runtime del árbol resuelto, no imágenes Docker, plugins de construcción ni dependencias exclusivas de pruebas.

## Tamaño y límites

El paquete inicial de Angular pasó de aproximadamente **563,40 kB a 573,00 kB**; su transferencia estimada pasó de **133,09 kB a 135,34 kB**. La actualización resuelve los avisos de dependencias, pero no reduce el arranque de la aplicación. Continúan las advertencias de estilos de navbar, galería y detalle del grupo. No se modificaron los presupuestos para ocultarlas.

No se añadieron migraciones ni funcionalidades nuevas. No se publicó una versión estable ni se ejecutó el despliegue.

## Repetir las comprobaciones

Desde la raíz, con Node/npm disponibles:

```powershell
npm ci
npm audit --audit-level=low
npm test -- --watch=false
npm run build
```

Con Java disponible, la auditoría de Maven puede ejecutarse desde la raíz:

```powershell
./scripts/audit-backend-dependencies.ps1
```

El script escribe `backend/target/dependency-audit-osv.json`. Devuelve código 0 si la consulta completa no devuelve alertas, 1 si encuentra paquetes afectados y 2 si no puede completar la comprobación.

Para verificar pruebas y construir el backend:

```powershell
cd backend
./mvnw.cmd verify
```

Para aplicar los cambios en Docker:

```powershell
docker compose up -d --build
```

Las siguientes etapas son los recorridos visuales en escritorio y celular, y la optimización de galerías grandes y carga inicial. La versión estable continúa aplazada.
