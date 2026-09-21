package edu.deploylab;

import java.util.*;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController @RequestMapping("/api/assignments")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.extras-enabled",havingValue="true")
public class AssignmentController {
    public record Deadline(@NotNull @Future OffsetDateTime dueAt) {}
    private final AssignmentService service;
    public AssignmentController(AssignmentService service) {this.service=service;}
    @GetMapping public Object list(@AuthenticationPrincipal AuthService.Actor user,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {return service.list(user,page,size);}
    @GetMapping("/{id}") public Object detail(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user) {return service.detail(id,user);}
    @PostMapping("/{id}/attempts") @ResponseStatus(HttpStatus.CREATED)
    public Object start(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user,@Valid @RequestBody AttemptController.Start body) {return Map.of("id",service.start(id,user,body.scenarioId()));}
    @PatchMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user,@Valid @RequestBody Deadline body) {service.update(id,user,body.dueAt());}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user) {service.cancel(id,user);}
    @GetMapping("/{id}/submissions") public Object submissions(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {return service.submissions(id,user,page,size);}
    @GetMapping("/{id}/submissions/{attempt}") public Object submission(@PathVariable UUID id,@PathVariable UUID attempt,@AuthenticationPrincipal AuthService.Actor user) {return service.submission(id,attempt,user);}
}
