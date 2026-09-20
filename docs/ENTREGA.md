# Guion para presentar el backend

## Recorrido

1. Mostrar `git log --oneline --reverse`. Comparar `0337c10` (pruebas del motor fallidas) con `d461463` (implementación inicial exitosa). Son etapas históricas: el contrato actual exige finalizar explícitamente.
2. Mostrar `6264588`: pruebas del contrato solicitado por el profesor antes de implementar las nuevas rutas. Compararlo con `e20f74f`, que implementa el núcleo y pasa las pruebas.
3. Ejecutar `./mvnw.cmd '-Dtest=CoreContractTest,SimulationEngineTest,TransactionRollbackTest' test`. Una prueba de rechazo pasa cuando obtiene el error esperado; un reporte histórico RED documenta una prueba que realmente falló al desarrollar.
4. Iniciar `./scripts/Start-Local.ps1` y ejecutar `./scripts/Demo.ps1`, o importar la colección Postman y recorrerla en orden.
5. Mostrar nota 90 tras una acción incorrecta y una correcta, los dos eventos y la fecha de evaluación. Repetir finalizar y mostrar que no cambia. Mostrar también la entrega sin resolver con cero puntos.

## Código que conviene explicar

- `CoreController` y `AuthController`: rutas del contrato y registro.
- `AttemptService.action`: propietario, bloqueo, idempotencia, actualización e inserción del evento en una transacción.
- `AttemptService.finish`: bloqueo compartido con acciones, cálculo, evaluación única y cierre.
- `SimulationEngine`: transición y evaluación separadas.
- Migraciones V4 y V5: evaluaciones explícitas y orden persistido de eventos.
- `TransactionRollbackTest`: evento demasiado largo provoca error en la base y revierte la actualización del estado.
- `CoreContractTest`: siete rutas, errores, concurrencia, idempotencia y módulos secundarios apagados.

## Aceptación

| Caso | Resultado |
|---|---|
| Registro válido | 201 |
| Login válido | 200 y token |
| Credenciales incorrectas o sin token | 401 |
| Taller | Evidencias y acciones sin solución |
| Iniciar intento | 201 y propietario autenticado |
| Acción permitida incorrecta | 200, evento y error registrado |
| Acción no disponible | 400 sin modificar datos |
| Intento ajeno | 404 |
| Repetir clave y acción | Mismo evento sin duplicados |
| Misma clave y distinta acción | 409 |
| Finalizar vacío | 409 |
| Finalizar resuelto | Nota e historial persistidos |
| Finalizar sin resolver | Cero puntos y entrega cerrada |
| Finalizar simultáneamente | Una sola evaluación |
| Acción nueva después de cerrar | 409 |
| Falla al guardar evento | Rollback completo |

## Entrega del historial

El repositorio no tiene remoto. Para abrir el historial portable, el profesor puede ejecutar:

```text
git clone DeployLab_Backend_Historial.bundle deploylab-con-historial
```

El núcleo no depende de SMTP, AWS ni módulos secundarios. La atribución de los commits permanece como Codex y las fechas corresponden a ejecuciones reales.
