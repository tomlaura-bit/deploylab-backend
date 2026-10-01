package edu.deploylab;

import edu.deploylab.dto.WorkshopCreateRequest;
import java.util.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.extras-enabled",havingValue="true")
public class CatalogController {
    private final CatalogService catalog;
    public CatalogController(CatalogService catalog) {this.catalog=catalog;}
    @GetMapping("/workshops") public Object list(@RequestParam(defaultValue="") String topic,@RequestParam(defaultValue="") String difficulty,
        @RequestParam(defaultValue="") String skill,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return catalog.list(topic,difficulty,skill,page,size);
    }
    @GetMapping("/workshops/{id}") public Object workshop(@PathVariable UUID id) {return catalog.workshop(id);}
    @GetMapping("/scenarios/{id}") public Object scenario(@PathVariable UUID id) {return catalog.scenario(id);}
    @GetMapping("/templates") @PreAuthorize("hasRole('INSTRUCTOR')") public Object templates(@AuthenticationPrincipal AuthService.Actor user) {
        user.instructor();return CatalogService.TEMPLATES;
    }
    @PostMapping("/workshops") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) @PreAuthorize("hasRole('INSTRUCTOR')")
    public Object create(@AuthenticationPrincipal AuthService.Actor user,@Valid @RequestBody WorkshopCreateRequest body) {
        return Map.of("id",catalog.create(user,new CatalogService.CreateWorkshop(body.title(),body.description(),body.topic(),body.difficulty(),body.templates())));
    }
}
