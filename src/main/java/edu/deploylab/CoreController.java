package edu.deploylab;

import java.util.*;
import edu.deploylab.dto.*;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

/** Canonical routes prioritized for the course deliverable. */
@RestController
@RequestMapping({"", "/api/v1"})
@Tag(name="Núcleo de DeployLab",description="Catálogo, acciones transaccionales y evaluación explícita")
public class CoreController {
    private final AuthService auth;
    private final CatalogService catalog;
    private final AttemptService attempts;
    private final ApiMapper mapper;
    public CoreController(AuthService auth,CatalogService catalog,AttemptService attempts,ApiMapper mapper) {
        this.auth=auth;this.catalog=catalog;this.attempts=attempts;this.mapper=mapper;
    }
    @PostMapping("/login")
    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    public AuthSessionResponse login(@Valid @RequestBody LoginRequest body) {return mapper.session(auth.login(mapper.login(body)));}
    @GetMapping("/talleres")
    public List<ApiResourceResponse> list(@RequestParam(defaultValue="") String topic,@RequestParam(defaultValue="") String difficulty,
        @RequestParam(defaultValue="") String skill,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return mapper.resources(catalog.list(topic,difficulty,skill,page,size),"/api/v1/talleres");
    }
    @GetMapping("/talleres/{id}")
    @Operation(summary="Obtener taller, evidencias y acciones disponibles de sus escenarios")
    public ApiResourceResponse workshop(@PathVariable UUID id) {return mapper.resource(catalog.workshop(id),"/api/v1/talleres/"+id);}
    @PostMapping("/escenarios/{id}/intentos") @ResponseStatus(HttpStatus.CREATED)
    public IdResponse start(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user) {
        UUID attempt=attempts.start(user,id);return IdResponse.of(attempt,"/api/v1/intentos/"+attempt);
    }
    @PostMapping("/intentos/{id}/acciones")
    @Operation(summary="Registrar una acción",description="Idempotency-Key debe ser un UUID nuevo por acción. Reenviar la misma clave y acción devuelve el evento original.")
    public ApiResourceResponse action(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user,
        @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody AttemptActionRequest body) {
        return mapper.resource(attempts.action(id,user,key,body.code()),"/api/v1/intentos/"+id);
    }
    @PostMapping("/intentos/{id}/finalizar")
    @Operation(summary="Cerrar intento y persistir evaluación e historial",description="Se requiere al menos una acción. Repetir la finalización devuelve la misma evaluación. No acepta una nota enviada por el cliente.")
    @SuppressWarnings("unchecked")
    public ApiResourceResponse finish(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user) {return mapper.resource((Map<String,Object>)attempts.finish(id,user),"/api/v1/intentos/"+id);}
    @GetMapping("/intentos")
    @SuppressWarnings("unchecked")
    public List<ApiResourceResponse> history(@AuthenticationPrincipal AuthService.Actor user,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {return mapper.resources((List<Map<String,Object>>)attempts.list(user,page,size),"/api/v1/intentos");}
    @GetMapping("/intentos/{id}")
    @SuppressWarnings("unchecked")
    public ApiResourceResponse detail(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user) {return mapper.resource((Map<String,Object>)attempts.detail(id,user),"/api/v1/intentos/"+id);}
}
