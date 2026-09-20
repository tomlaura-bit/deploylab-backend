package edu.deploylab;

import java.util.*;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

/** Canonical routes prioritized for the course deliverable. */
@RestController
@Tag(name="Núcleo de DeployLab",description="Catálogo, acciones transaccionales y evaluación explícita")
public class CoreController {
    private final AuthService auth;
    private final CatalogService catalog;
    private final AttemptService attempts;
    public CoreController(AuthService auth,CatalogService catalog,AttemptService attempts) {
        this.auth=auth;this.catalog=catalog;this.attempts=attempts;
    }
    @PostMapping("/login")
    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    public AuthService.Session login(@Valid @RequestBody AuthService.Login body) {return auth.login(body);}
    @GetMapping("/talleres")
    public Object list(@RequestParam(defaultValue="") String topic,@RequestParam(defaultValue="") String difficulty,
        @RequestParam(defaultValue="") String skill,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return catalog.list(topic,difficulty,skill,page,size);
    }
    @GetMapping("/talleres/{id}")
    @Operation(summary="Obtener taller, evidencias y acciones disponibles de sus escenarios")
    public Object workshop(@PathVariable UUID id) {return catalog.workshop(id);}
    @PostMapping("/escenarios/{id}/intentos") @ResponseStatus(HttpStatus.CREATED)
    public Object start(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user) {
        return Map.of("id",attempts.start(user,id));
    }
    @PostMapping("/intentos/{id}/acciones")
    @Operation(summary="Registrar una acción",description="Idempotency-Key debe ser un UUID nuevo por acción. Reenviar la misma clave y acción devuelve el evento original.")
    public Object action(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user,
        @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody AttemptController.Action body) {
        return attempts.action(id,user,key,body.code());
    }
    @PostMapping("/intentos/{id}/finalizar")
    @Operation(summary="Cerrar intento y persistir evaluación e historial",description="Se requiere al menos una acción. Repetir la finalización devuelve la misma evaluación. No acepta una nota enviada por el cliente.")
    public Object finish(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user) {return attempts.finish(id,user);}
    @GetMapping("/intentos")
    public Object history(@AuthenticationPrincipal AuthService.Actor user,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {return attempts.list(user,page,size);}
    @GetMapping("/intentos/{id}")
    public Object detail(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user) {return attempts.detail(id,user);}
}
