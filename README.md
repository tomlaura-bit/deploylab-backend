# DeployLab Backend

Backend de la propuesta aprobada para CS 2031 DBP: plataforma educativa para practicar el diagnóstico de errores mediante escenarios simulados. El alumno registra acciones, recibe una evaluación y consulta su historial. El instructor crea talleres, organiza grupos, asigna trabajo y revisa entregas.

Se implementaron los **siete endpoints enumerados por el profesor** y se ampliaron los módulos de gestión. La entrega incluye código, base de datos, pruebas, Swagger, Postman, demostraciones y un historial Git real. No incluye una interfaz gráfica de alumno y no se ha desplegado en Vercel.

## 1. Qué se ha hecho hasta ahora

| Módulo | Funcionalidad implementada |
|---|---|
| Autenticación | Registro de estudiantes, login, sesión actual, logout, contraseñas BCrypt y tokens revocables con vencimiento |
| Catálogo | Talleres publicados, filtros por tema/dificultad/habilidad, paginación, escenarios y acciones sin revelar respuestas |
| Simulador | Intentos, eventos transaccionales, validación de estados, pistas, idempotencia y control de concurrencia |
| Evaluación | Finalización explícita, nota persistida, explicación, historial ordenado y progreso por habilidad |
| Instructor | Crear talleres desde plantillas, listar los propios, editar metadatos, publicar/despublicar y consultar materiales pendientes |
| Grupos y miembros | Crear, consultar, renombrar, archivar/reactivar, añadir estudiantes por ID o correo y retirar miembros |
| Asignaciones | Asignar taller, consultar tareas, cambiar plazo, cancelar e iniciar intentos vinculados a una asignación |
| Revisión | Consultar entregas finalizadas, evaluación, eventos e indicador de entrega tardía |
| Seguimiento | Panel por rol, progreso personal, estadísticas del grupo y separación de prácticas personales y entregas |
| Materiales | Subir PDF a PostgreSQL, listar, descargar y retirar; flujo opcional de carga firmada a S3 |
| Reportes | CSV personal y de grupo, procesamiento en segundo plano, estado, descarga y reintento de trabajos fallidos |
| Notificaciones | Aviso interno al asignar taller, filtro de no leídas, paginación, marcar una o todas como leídas y correo opcional |

La ampliación corrigió un problema: las estadísticas de un grupo podían incluir prácticas personales de sus miembros. Ahora la entrega se vincula explícitamente mediante `assignment_id`; las prácticas iniciadas desde el núcleo no aparecen en reportes del grupo.

## 2. Tecnologías y organización

Java 21, Spring Boot 3.5.7, PostgreSQL 17, Spring Security, JDBC (`JdbcTemplate`), Flyway, Bean Validation, Springdoc/OpenAPI, JUnit, MockMvc y JaCoCo. Maven Wrapper permite compilar sin instalar Maven por separado.

```text
src/main/java/edu/deploylab/    Controladores, servicios, seguridad y motor
src/main/resources/            Configuración y migraciones SQL
src/test/java/edu/deploylab/    Pruebas de API, motor e integraciones
scripts/                      Inicio, parada y demos locales
docs/                        Postman, guion y evidencias
.github/workflows/ci.yml       CI preparada para PostgreSQL
```

Los controladores validan solicitudes; los servicios aplican permisos y reglas de negocio. PostgreSQL conserva usuarios, talleres, escenarios, intentos, eventos, evaluaciones, grupos, miembros, asignaciones, materiales, avisos y trabajos. El motor usa transacciones y bloqueos de fila.

Las migraciones son incrementales: V1 crea el esquema; V2 asocia habilidades; V3 introduce el estado de materiales; V4 persiste evaluaciones; V5 ordena eventos; V6 añade asignaciones explícitas, archivado, cancelación y PDF locales. No se modificaron migraciones ya aplicadas. Los intentos anteriores a V6 mantienen `assignment_id=null`: no se atribuyen retrospectivamente a grupos.

## 3. Ejecutar en Windows

Requisitos: Java 21 y PostgreSQL 17 instalado. La primera compilación necesita Internet para descargar dependencias. Desde esta carpeta en PowerShell:

```powershell
.\scripts\Start-Local.ps1
.\scripts\Demo.ps1
.\scripts\Demo-Modules.ps1
```

La API escucha en `http://127.0.0.1:8080`. El script usa una base aislada en `data/postgres`, puerto `55440`, sin modificar las bases del servicio PostgreSQL existente. Conserva los datos entre reinicios. El instructor inicial y las credenciales aleatorias quedan en `data/local-config.json`, excluido de Git. Las demos crean datos ficticios nuevos y no imprimen tokens ni contraseñas.

[Swagger interactivo](http://localhost:8080/swagger-ui/index.html): selecciona **core** para los siete endpoints o **modules** para `/api`. Especificaciones: `/v3/api-docs/core` y `/v3/api-docs/modules`. Inicia sesión y pega el valor de `token` en **Authorize**. El registro público siempre crea estudiantes; las credenciales del instructor están en el archivo local indicado.

```powershell
# Detener conservando datos
.\scripts\Stop-Local.ps1
# Iniciar solo el núcleo
.\scripts\Start-Local.ps1 -CoreOnly
# Puerto HTTP alternativo
.\scripts\Start-Local.ps1 -ApiPort 8085
.\scripts\Demo.ps1 -BaseUrl http://127.0.0.1:8085
```

Detén la instancia antes de cambiar de modo. Los módulos están **habilitados por defecto** (`EXTRAS_ENABLED=true`). El modo núcleo conserva tablas, pero no expone controladores opcionales ni ejecuta el trabajador de reportes. El puerto de PostgreSQL debe coincidir con el guardado al inicializar la base.

## 4. Contrato prioritario del profesor

Todas las rutas salvo registro y login requieren `Authorization: Bearer <token>`. Los cuerpos usan campos en inglés; las respuestas JDBC conservan nombres SQL como `created_at`.

| Método | Ruta | Uso |
|---|---|---|
| POST | `/auth/register` | `name`, `email`, `password`; crea estudiante |
| POST | `/login` | `email`, `password`; devuelve token y usuario |
| GET | `/talleres` | Filtros `topic`, `difficulty`, `skill`; `page`, `size` |
| GET | `/talleres/{id}` | Taller, escenarios, evidencias y acciones |
| POST | `/escenarios/{id}/intentos` | Iniciar práctica personal; sin cuerpo |
| POST | `/intentos/{id}/acciones` | `{ "code": "FIX_URL" }` y `Idempotency-Key: <UUID>` |
| POST | `/intentos/{id}/finalizar` | Cerrar y devolver evaluación e historial; sin cuerpo |

El profesor los denominó seis, pero enumeró siete. `/auth/login` es alias de `/login`. `/auth/me`, `/auth/logout`, `/intentos` y `/intentos/{id}` completan sesión e historial. Se conservan los alias `/api/auth/*` y `/api/attempts/*`.

Taller inicial: `10000000-0000-0000-0000-000000000001`. Escenarios: API mal configurada, credenciales ficticias incorrectas y permisos insuficientes. Sus IDs se consultan en el detalle del taller.

## 5. Estados y evaluación

1. Inicio: `state=IN_PROGRESS`, `finalized=false`, `score=0`.
2. Acción incorrecta permitida: registra evento, suma un error y mantiene el estado.
3. Acción correcta: registra evento y cambia a `RESOLVED`. Todavía no calcula la nota.
4. Finalizar exige una acción como mínimo, persiste evaluación y fecha de cierre. Entregar sin resolver produce cero puntos.
5. El cierre rechaza acciones nuevas. Repetir finalizar devuelve la misma evaluación y fecha.

Nota resuelta: `max(0, 100 - 10 × errores - 5 × pistas)`. Sin resolver: 0. Repetir una pista no penaliza nuevamente. La fórmula es una decisión del proyecto, no una rúbrica del profesor.

`state` describe el incidente; `finalized` describe la entrega. Sin resolver puede conservar `IN_PROGRESS` con `finalized=true` y `evaluation.solved=false`. Los eventos mantienen `score=0`; la nota oficial se calcula al finalizar.

El motor usa tres plantillas predefinidas. No ejecuta código arbitrario ni se conecta a las aplicaciones representadas. El instructor edita metadatos del taller; la API no permite alterar respuestas del escenario después de crearlo.

## 6. Rutas de los módulos ampliados

Todas requieren autenticación. Las operaciones de instructor comprueban rol y propiedad del recurso.

| Módulo | Rutas principales |
|---|---|
| Catálogo | `GET/POST /api/workshops`, `GET /api/workshops/{id}`, `GET /api/scenarios/{id}`, `GET /api/templates` |
| Instructor | `GET /api/instructor/workshops`, `GET/PATCH /api/instructor/workshops/{id}` |
| Grupos | `GET/POST /api/groups`, `GET/PATCH /api/groups/{id}` |
| Miembros | `POST /api/groups/{id}/members`, `POST /api/groups/{id}/members/by-email`, `DELETE /api/groups/{id}/members/{member}` |
| Asignar taller | `POST /api/groups/{id}/assignments` |
| Tareas | `GET /api/assignments`, `GET/PATCH/DELETE /api/assignments/{id}` |
| Intento asignado | `POST /api/assignments/{id}/attempts` |
| Revisión | `GET /api/assignments/{id}/submissions`, `GET /api/assignments/{id}/submissions/{attempt}` |
| Práctica | `GET/POST /api/attempts`, `GET /api/attempts/{id}`, `POST /api/attempts/{id}/actions`, `/hint`, `/finish` |
| Seguimiento | `GET /api/dashboard`, `GET /api/progress`, `GET /api/groups/{id}/statistics` |
| Materiales | `GET /api/workshops/{id}/materials`, `POST /api/workshops/{id}/materials/local` |
| Archivo | `GET /api/workshops/{id}/materials/{material}/content`, `GET .../{material}/download`, `DELETE .../{material}` |
| S3 opcional | `POST /api/workshops/{id}/materials`, `POST .../{material}/confirm` |
| Reportes | `POST /api/reports`, `POST /api/groups/{id}/reports` |
| Trabajos | `GET /api/jobs`, `GET /api/jobs/{id}`, `GET /api/jobs/{id}/download`, `POST /api/jobs/{id}/retry` |
| Avisos | `GET /api/notifications`, `PATCH /api/notifications/{id}/read`, `PATCH /api/notifications/read-all` |

Listados de talleres, intentos, asignaciones, entregas, trabajos y avisos aceptan `page` y `size` (0 y 20 por defecto, máximo 100). Avisos admite `unreadOnly=true`. Grupos y miembros se devuelven completos.

### Ejemplo de taller y asignación

Como instructor, crear con `POST /api/workshops`:

```json
{
  "title": "Diagnóstico de servicios",
  "description": "Resolver tres incidentes simulados",
  "topic": "Backend",
  "difficulty": "BEGINNER",
  "templates": ["API_URL", "DB_AUTH", "PERMISSIONS"]
}
```

El taller se publica inmediatamente. `PATCH /api/instructor/workshops/{id}` recibe `title`, `description`, `topic`, `difficulty` y `published`. El propietario consulta talleres despublicados en la ruta de instructor. Despublicar impide iniciar intentos nuevos; los existentes conservan historial y pueden finalizarse.

Crear grupo con `{ "name": "DBP Grupo 1" }`. Añadir estudiante registrado con `userId` en `/members` o `email` en `/members/by-email`. Asignar taller:

```json
{
  "workshopId": "10000000-0000-0000-0000-000000000001",
  "dueAt": "2030-12-15T23:59:00-05:00"
}
```

Usa una fecha futura real del curso. El alumno consulta `/api/assignments` y comienza mediante `/api/assignments/{id}/attempts` con `{ "scenarioId": "<UUID>" }`. Acciones y cierre usan el mismo simulador; el servidor establece la asociación con la tarea.

### Reglas de grupos, plazos y privacidad

- Solo el propietario administra el grupo. El alumno no recibe correos de compañeros.
- Un grupo archivado impide añadir alumnos, asignar talleres e iniciar intentos asignados. `PATCH` con `name` y `archived=false` lo reactiva.
- Solo un estudiante miembro inicia un intento asignado. El escenario pertenece al taller y el plazo debe seguir vigente.
- Cambiar plazo: `PATCH /api/assignments/{id}` con `dueAt`. `DELETE` cancela sin borrar historial. Un mismo taller tiene una asignación por grupo, incluso cancelada.
- Un intento ya iniciado puede terminar después del plazo, archivado o cancelación. La lista de entregas calcula `late` respecto al plazo actual; al ampliarlo puede cambiar ese indicador.
- Retirar un alumno corta acceso al grupo y a nuevas tareas; conserva sus intentos. El instructor mantiene acceso a entregas anteriores. Estadísticas y CSV del grupo consideran miembros actuales.
- El instructor revisa solo intentos finalizados vinculados explícitamente a su asignación; las prácticas personales no son entregas del grupo.

### Materiales, reportes y avisos

Carga local: `multipart/form-data`, campo `file`, nombre como `guia.pdf`, máximo 10 MiB. Verifica nombre y cabecera `%PDF-`, sin análisis antivirus ni validación exhaustiva del PDF. Los bytes se guardan en PostgreSQL. La descarga requiere token; su `downloadUrl` local es una ruta relativa autenticada. Retirar oculta el material y elimina sus bytes locales.

S3: solicitar URL → subir PDF → confirmar → descargar. La confirmación valida tamaño y tipo MIME. Retirar material S3 lo oculta en la aplicación sin borrar el objeto externo. Un enlace firmado ya emitido puede seguir válido durante sus cinco minutos.

Reportes: POST devuelve `202` y un ID; consultar hasta `SUCCEEDED`, después descargar CSV. El trabajador intenta hasta tres veces; `/retry` reinicia solo un trabajo propio en `FAILED`. Las celdas CSV se protegen frente a interpretación como fórmulas.

Al asignar un taller se notifican los estudiantes presentes. En ejecución local estándar SMTP está desactivado: los trabajos de correo terminan en `FAILED` tras reintentos, sin impedir el aviso interno ni la asignación. No se informa de un correo enviado si no se entregó al proveedor.

## 7. Seguridad y consistencia

- BCrypt; tokens aleatorios de 256 bits almacenados como SHA-256, vigentes ocho horas y revocables al salir.
- El registro no permite elegir rol; el instructor inicial se configura por entorno. Un intento ajeno responde 404.
- Acciones y cierre bloquean la fila (`SELECT FOR UPDATE`). Estado, eventos y evaluación se guardan en transacciones; si falla insertar un evento se revierte el cambio de estado.
- Idempotencia: misma clave y acción devuelven el evento previo; otra acción con esa clave responde 409.
- La finalización concurrente produce una evaluación. Eventos ordenados con índice persistido.
- SQL parametrizado; validación de campos, UUID y paginación; rechazo de propiedades desconocidas; CORS configurable.

## 8. Pruebas y evidencia

```powershell
# Suite completa con H2 en modo PostgreSQL
.\mvnw.cmd verify
# Núcleo y transacciones
.\mvnw.cmd '-Dtest=CoreContractTest,SimulationEngineTest,TransactionRollbackTest' test
# Ampliación
.\mvnw.cmd '-Dtest=ExpandedModulesTest' test
```

Para PostgreSQL utiliza una base exclusiva de pruebas:

```powershell
$env:TEST_DB_URL='jdbc:postgresql://127.0.0.1:5432/deploylab_test'
$env:TEST_DB_USER='deploylab'
$env:TEST_DB_PASSWORD='<clave de pruebas>'
.\mvnw.cmd verify
```

Las pruebas insertan datos ficticios; no apuntes a una base de uso real. Flyway aplica el esquema. Reportes: `target/surefire-reports`; cobertura: `target/site/jacoco/index.html`.

Verificación final: **39 pruebas aprobadas en H2 y 39 en PostgreSQL 17**, sin fallos ni omisiones. La demo HTTP del núcleo y de los módulos ampliados también pasó, junto con las 16 solicitudes y 23 comprobaciones de Postman. Swagger expone 7 rutas en core y 44 en modules. Evidencias: `11-modules-h2.txt`, `12-modules-postgres.txt` y `13-modules-http.txt`.

Se prueban credenciales/roles, catálogo, motor, puntuación, pistas, idempotencia, privacidad, concurrencia, rollback, grupos, asignaciones, publicación, materiales, avisos, trabajos e integraciones simuladas. `docs/evidence` conserva resultados históricos RED/GREEN y la verificación de esta ampliación. Guion: `docs/ENTREGA.md`.

La colección `docs/DeployLab.postman_collection.json` verifica el núcleo en orden. `Demo-Modules.ps1` verifica por HTTP instructor → alumno → asignación → entrega → reporte. La CI está preparada con PostgreSQL, pero no se ha ejecutado en un remoto.

## 9. Commits para el profesor

```powershell
git log --oneline --reverse
git show 0337c10
git show d461463
git show 6264588
git show e20f74f
git show 5152675
git show d62bc9e
```

Hay commits reales de pruebas fallidas antes de implementar y correcciones posteriores. `5152675` demuestra la falta de carga local PDF y la inclusión incorrecta de prácticas personales en estadísticas. `d62bc9e` implementa la ampliación y conserva las evidencias de las pruebas aprobadas. Una prueba de rechazo que pasa al recibir 403/404 es distinta de una ejecución RED que realmente falla.

Autor de los commits: **Codex**, con fechas reales. El repositorio es local, sin remoto. Para recuperar el historial portable:

```text
git clone DeployLab_Backend_Historial.bundle deploylab-con-historial
```

## 10. Configuración y límites actuales

| Variable | Uso |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | PostgreSQL |
| `PORT` | HTTP, 8080 por defecto |
| `EXTRAS_ENABLED` | Módulos ampliados, `true` por defecto |
| `INSTRUCTOR_EMAIL`, `INSTRUCTOR_PASSWORD` | Instructor inicial si no existe |
| `CORS_ORIGIN` | Origen permitido; `http://localhost:5173` por defecto |
| `MAIL_ENABLED`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_TLS`, `MAIL_FROM` | Correo opcional |
| `S3_BUCKET`, `AWS_REGION` | S3 opcional; credenciales por la cadena estándar del SDK AWS |

Con base existente: configurar variables y ejecutar `./mvnw.cmd spring-boot:run`. Con Docker Desktop: copiar `.env.example` a `.env`, configurar credenciales y ejecutar `docker compose up --build`. Incluye PostgreSQL y Mailpit para capturar correo localmente. Docker es una alternativa incluida, no la vía usada para validar esta entrega.

El backend cubre los flujos académicos descritos. SMTP y S3 tienen pruebas con dobles de servicio y casos de indisponibilidad, sin validación de una cuenta externa real. Quedan fuera frontend, recuperación de contraseña, verificación de correo, administración general de roles, editor libre de escenarios y preparación operativa para alta carga. PDF en PostgreSQL simplifica la entrega local; S3 permite separar archivos al crecer el volumen.
