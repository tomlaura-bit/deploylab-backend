package edu.deploylab;

import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;

@RestController @RequestMapping({"/api/instructor/workshops","/api/v1/instructor/workshops"})
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.extras-enabled",havingValue="true")
public class InstructorController {
    public record Edit(@NotBlank @Size(max=150) String title,@NotBlank @Size(max=2000) String description,
        @NotBlank @Size(max=60) String topic,@NotNull @Pattern(regexp="BEGINNER|INTERMEDIATE") String difficulty,@NotNull Boolean published) {}
    private final Db db; private final CatalogService catalog;
    public InstructorController(Db db,CatalogService catalog) {this.db=db;this.catalog=catalog;}
    @GetMapping @PreAuthorize("hasRole('INSTRUCTOR')") public Object list(@AuthenticationPrincipal AuthService.Actor user,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        user.instructor();AssignmentService.page(page,size);
        return db.query("SELECT * FROM workshop WHERE owner_id=? ORDER BY title,id LIMIT ? OFFSET ?",user.id(),size,page*size);
    }
    @GetMapping("/{id}") @PreAuthorize("hasRole('INSTRUCTOR')") public Object detail(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user) {
        catalog.requireOwner(id,user);var w=db.one("SELECT * FROM workshop WHERE id=?",id);
        w.put("scenarios",db.query("SELECT * FROM scenario WHERE workshop_id=? ORDER BY title",id));
        w.put("materials",db.query("SELECT id,filename,status FROM material WHERE workshop_id=? AND deleted=FALSE ORDER BY filename,id",id));return w;
    }
    @PatchMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional @PreAuthorize("hasRole('INSTRUCTOR')")
    public void edit(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user,@Valid @RequestBody Edit body) {
        catalog.requireOwner(id,user);
        db.update("UPDATE workshop SET title=?,description=?,topic=?,difficulty=?,published=? WHERE id=?",body.title(),body.description(),body.topic(),body.difficulty(),body.published(),id);
    }
}
