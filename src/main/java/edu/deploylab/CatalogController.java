package edu.deploylab;

import java.util.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api")
public class CatalogController {
    private final CatalogService catalog;
    public CatalogController(CatalogService catalog) {this.catalog=catalog;}
    @GetMapping("/workshops") public Object list(@RequestParam(defaultValue="") String topic,@RequestParam(defaultValue="") String difficulty,
        @RequestParam(defaultValue="") String skill,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return catalog.list(topic,difficulty,skill,page,size);
    }
    @GetMapping("/workshops/{id}") public Object workshop(@PathVariable UUID id) {return catalog.workshop(id);}
    @GetMapping("/scenarios/{id}") public Object scenario(@PathVariable UUID id) {return catalog.scenario(id);}
    @GetMapping("/templates") public Object templates(@AuthenticationPrincipal AuthService.Actor user) {
        user.instructor();return CatalogService.TEMPLATES;
    }
    @PostMapping("/workshops") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Object create(@AuthenticationPrincipal AuthService.Actor user,@Valid @RequestBody CatalogService.CreateWorkshop body) {
        return Map.of("id",catalog.create(user,body));
    }
}
