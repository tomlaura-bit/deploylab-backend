package edu.deploylab;

import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/workshops/{workshop}/materials")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.extras-enabled",havingValue="true")
public class MaterialController {
    public record Upload(@NotBlank @Pattern(regexp="[a-zA-Z0-9_-]{1,100}\\.pdf") String filename) {}
    private final Db db;private final CatalogService catalog;private final S3Storage storage;
    public MaterialController(Db db,CatalogService catalog,S3Storage storage) {this.db=db;this.catalog=catalog;this.storage=storage;}
    @GetMapping public Object list(@PathVariable UUID workshop) {catalog.workshop(workshop);return db.jdbc.queryForList("SELECT id,filename FROM material WHERE workshop_id=? AND status='READY'",workshop);}
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Object upload(@PathVariable UUID workshop,@AuthenticationPrincipal AuthService.Actor user,@Valid @RequestBody Upload body) {
        catalog.requireOwner(workshop,user); UUID id=UUID.randomUUID();
        String key="workshops/"+workshop+"/"+id+"/"+body.filename();
            String url=storage.uploadUrl(key);
            db.jdbc.update("INSERT INTO material(id,workshop_id,filename,object_key) VALUES (?,?,?,?)",id,workshop,body.filename(),key);
            return Map.of("id",id,"uploadUrl",url,"contentType","application/pdf","expiresInSeconds",300);
    }
    @PostMapping("/{id}/confirm") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void confirm(@PathVariable UUID workshop,@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user) {
        catalog.requireOwner(workshop,user);
        var material=db.one("SELECT object_key FROM material WHERE id=? AND workshop_id=?",id,workshop);
        storage.verifyUpload(material.get("object_key").toString());
        db.jdbc.update("UPDATE material SET status='READY' WHERE id=?",id);
    }
    @GetMapping("/{id}/download") public Object download(@PathVariable UUID workshop,@PathVariable UUID id) {
        catalog.workshop(workshop);var material=db.one("SELECT object_key FROM material WHERE id=? AND workshop_id=? AND status='READY'",id,workshop);
        return Map.of("downloadUrl",storage.downloadUrl(material.get("object_key").toString()));
    }
}
