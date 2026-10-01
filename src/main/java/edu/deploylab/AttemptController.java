package edu.deploylab;

import edu.deploylab.dto.*;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.extras-enabled",havingValue="true")
public class AttemptController {
    public record Start(@NotNull UUID scenarioId) {}
    public record Action(@NotBlank @Size(max=60) String code) {}
    private final AttemptService attempts;
    public AttemptController(AttemptService attempts) {this.attempts=attempts;}
    @PostMapping("/attempts") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Object start(@AuthenticationPrincipal AuthService.Actor user,@Valid @RequestBody AttemptStartRequest body) {return Map.of("id",attempts.start(user,body.scenarioId()));}
    @GetMapping("/attempts") public Object list(@AuthenticationPrincipal AuthService.Actor user,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {return attempts.list(user,page,size);}
    @GetMapping("/attempts/{id}") public Object detail(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user) {return attempts.detail(id,user);}
    @PostMapping("/attempts/{id}/actions") public Object action(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user,
        @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody AttemptActionRequest body) {return attempts.action(id,user,key,body.code());}
    @PostMapping("/attempts/{id}/hint") public Object hint(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user) {return attempts.hint(id,user);}
    @PostMapping("/attempts/{id}/finish") public Object finish(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user) {return attempts.finish(id,user);}
    @GetMapping("/progress") public Object progress(@AuthenticationPrincipal AuthService.Actor user) {return attempts.progress(user);}
}
