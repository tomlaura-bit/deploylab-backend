# DeployLab Backend

Backend de la propuesta aprobada para CS 2031 DBP. Java 21, Spring Boot 3.5.7, PostgreSQL, Spring Security, JDBC y migraciones Flyway. La prioridad son los **siete endpoints enumerados por el profesor** (su mensaje los denomina seis).

## Iniciar y demostrar en Windows

Requisitos: Java 21 y PostgreSQL 17 instalado. La primera compilación necesita Internet para descargar Maven y sus dependencias. Desde esta carpeta, en PowerShell:

```powershell
.\scripts\Start-Local.ps1
.\scripts\Demo.ps1
```

La API inicia en `http://localhost:8080` con una base aislada en `data/postgres`, puerto `55440`. No modifica las bases del servicio PostgreSQL existente. Las credenciales generadas quedan en `data/local-config.json`, excluido de Git. La demo ejecuta el recorrido completo y verifica tanto respuestas exitosas como errores esperados.

Swagger: http://localhost:8080/swagger-ui/index.html . El grupo **core** muestra únicamente los siete endpoints. La especificación está en `/v3/api-docs/core`. Importa `docs/DeployLab.postman_collection.json` en Postman y ejecuta las peticiones en orden.

Para detener la instancia conservando los datos:

```powershell
.\scripts\Stop-Local.ps1
```

Puedes iniciar con `-ApiPort 8085` y ejecutar la demo con `-BaseUrl http://localhost:8085`. El puerto PostgreSQL debe coincidir con el guardado al inicializar la base.

## Contrato prioritario

Todos salvo registro y login requieren `Authorization: Bearer <token>`. Los cuerpos y respuestas usan campos en inglés.

| Método | Ruta | Función |
|---|---|---|
| POST | `/auth/register` | Crear estudiante con `name`, `email`, `password`; devuelve 201 |
| POST | `/login` | Autenticar con `email`, `password`; devuelve token y usuario |
| GET | `/talleres` | Listar; filtros `topic`, `difficulty`, `skill` y paginación `page`, `size` |
| GET | `/talleres/{id}` | Escenarios, evidencias y acciones del taller sin revelar soluciones |
| POST | `/escenarios/{id}/intentos` | Iniciar intento propio; sin cuerpo; devuelve 201 e `id` |
| POST | `/intentos/{id}/acciones` | Registrar `{ "code": "FIX_URL" }` con `Idempotency-Key: <UUID>` |
| POST | `/intentos/{id}/finalizar` | Cerrar y devolver evaluación persistida e historial; sin cuerpo |

`/auth/login` es alias de `/login`. Las rutas auxiliares `GET /intentos`, `GET /intentos/{id}`, `GET /auth/me` y `POST /auth/logout` permiten recuperar el historial y gestionar la sesión.

El taller inicial tiene ID `10000000-0000-0000-0000-000000000001`. Los IDs de sus tres escenarios se obtienen del detalle del taller: API mal configurada, credenciales ficticias incorrectas y permisos insuficientes.

## Estados y evaluación

1. Inicio: `state=IN_PROGRESS`, `finalized=false`, `score=0`.
2. Acción permitida incorrecta: guarda evento, suma un error y mantiene `IN_PROGRESS`.
3. Acción correcta: guarda evento y cambia a `RESOLVED`. Todavía no calcula la nota.
4. Finalizar exige al menos una acción. Calcula y persiste la evaluación, marca `finalized=true` y devuelve el historial. Se puede entregar sin resolver, con nota cero.
5. Un intento finalizado rechaza acciones nuevas. Repetir finalizar devuelve la misma evaluación y fecha.

Nota de un incidente resuelto: `max(0, 100 - 10 × errores - 5 × pistas)`. Sin resolver: 0. En el núcleo no se exponen pistas, por lo que el descuento habitual es 10 puntos por error. Esta fórmula es una decisión del proyecto, no una rúbrica del profesor.

`state` describe el incidente; `finalized` describe la entrega. Una entrega sin resolver conserva `IN_PROGRESS` y tiene `finalized=true`, `evaluation.solved=false`. Las fechas de cierre y evaluación solo se completan al finalizar. Los eventos conservan `score=0`; la calificación oficial está en `evaluation`.

## Transacciones y seguridad

- BCrypt para contraseñas; tokens opacos de 256 bits guardados como SHA-256, vigentes ocho horas y revocables al salir.
- El registro no acepta roles. Un ID de intento ajeno responde 404.
- Acciones y finalización bloquean la fila (`SELECT FOR UPDATE`). Estado, eventos y evaluación se guardan en transacciones.
- Si falla guardar un evento, se revierte el cambio de estado. Una prueba fuerza esa falla en la base real.
- Clave de idempotencia única por intento: misma clave y acción devuelve el mismo evento; otra acción con esa clave produce 409.
- Finalización concurrente: una evaluación por intento. Después del cierre solo se aceptan relecturas o reintentos idénticos de eventos previos.
- Historial ordenado con un índice de evento persistido. Consultas SQL parametrizadas y validación de cuerpos, UUID y paginación.

## Pruebas

```powershell
# Suite completa con H2 en modo PostgreSQL
.\mvnw.cmd verify

# Solo el núcleo
.\mvnw.cmd '-Dtest=CoreContractTest,SimulationEngineTest,TransactionRollbackTest' test
```

Para PostgreSQL, crea una base exclusiva para pruebas y configura:

```powershell
$env:TEST_DB_URL='jdbc:postgresql://localhost:5432/deploylab_test'
$env:TEST_DB_USER='deploylab'
$env:TEST_DB_PASSWORD='<clave de pruebas>'
.\mvnw.cmd verify
```

No utilices una base con datos reales: las pruebas insertan usuarios, intentos y datos ficticios. Flyway aplica el esquema. Los resultados quedan en `target/surefire-reports`; la cobertura está en `target/site/jacoco/index.html`. La CI de GitHub está preparada para PostgreSQL, pero aún no se ha ejecutado en un remoto.

Verificación de esta entrega: **35 pruebas automatizadas aprobadas** sobre PostgreSQL 17 y sobre H2, demo HTTP exitosa y **16 solicitudes / 23 comprobaciones de Postman aprobadas**. Los reportes históricos y finales están en `docs/evidence`.

## Commits para el profesor

```powershell
git log --oneline --reverse
git show 0337c10
git show d461463
git show 6264588
```

Hay commits reales RED/GREEN y reportes de ejecución en `docs/evidence`. El autor es **Codex**, con fechas reales; no se atribuye trabajo ficticio al equipo. Lee `docs/ENTREGA.md` para el guion. El repositorio es local, sin remoto. **No se ha subido a Vercel.**

## Alcance y módulos secundarios

Grupos, membresías, asignaciones, reportes, notificaciones, S3, creación de talleres y las rutas antiguas `/api` quedan desactivados por defecto (`EXTRAS_ENABLED=false`). Se conservan código y tablas para no perder trabajo previo. Se pueden habilitar con `EXTRAS_ENABLED=true` si hay tiempo; no son dependencias del núcleo.

SMTP y S3 tienen pruebas con servicios simulados y casos de indisponibilidad; no se ha verificado una cuenta AWS real. Para uso real necesitan `MAIL_ENABLED`, `SMTP_*`, `S3_BUCKET`, `AWS_REGION` y credenciales mediante la cadena estándar del SDK.

Esta entrega incluye backend, Swagger, Postman y demo; no incluye una interfaz de alumno.

## Otras formas de ejecución

Con Docker Desktop iniciado: copiar `.env.example` a `.env` y ejecutar `docker compose up --build`. El Dockerfile y Compose son alternativas incluidas; la validación local se realiza con Java y PostgreSQL instalados.

Con una base existente: definir `DB_URL`, `DB_USER`, `DB_PASSWORD` y ejecutar `./mvnw.cmd spring-boot:run`. Para crear el instructor inicial opcional se usan `INSTRUCTOR_EMAIL` e `INSTRUCTOR_PASSWORD`; el núcleo del estudiante no requiere instructor.
