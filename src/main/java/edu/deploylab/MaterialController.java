package edu.deploylab;

import java.time.Duration;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.*;

@RestController @RequestMapping("/api/workshops/{workshop}/materials")
public class MaterialController {
    public record Upload(@NotBlank @Pattern(regexp="[a-zA-Z0-9_-]{1,100}\\.pdf") String filename) {}
    private final Db db;private final CatalogService catalog;
    @Value("${app.s3-bucket}") String bucket; @Value("${app.s3-region}") String region;
    public MaterialController(Db db,CatalogService catalog) {this.db=db;this.catalog=catalog;}
    @GetMapping public Object list(@PathVariable UUID workshop) {catalog.workshop(workshop);return db.jdbc.queryForList("SELECT id,filename FROM material WHERE workshop_id=?",workshop);}
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Object upload(@PathVariable UUID workshop,@AuthenticationPrincipal AuthService.Actor user,@Valid @RequestBody Upload body) {
        catalog.requireOwner(workshop,user); enabled(); UUID id=UUID.randomUUID();
        String key="workshops/"+workshop+"/"+id+"/"+body.filename();
        try(var signer=S3Presigner.builder().region(Region.of(region)).build()) {
            var req=PutObjectRequest.builder().bucket(bucket).key(key).contentType("application/pdf").build();
            String url=signer.presignPutObject(r->r.signatureDuration(Duration.ofMinutes(5)).putObjectRequest(req)).url().toString();
            db.jdbc.update("INSERT INTO material(id,workshop_id,filename,object_key) VALUES (?,?,?,?)",id,workshop,body.filename(),key);
            return Map.of("id",id,"uploadUrl",url,"contentType","application/pdf","expiresInSeconds",300);
        }
    }
    @GetMapping("/{id}/download") public Object download(@PathVariable UUID workshop,@PathVariable UUID id) {
        catalog.workshop(workshop);var material=db.one("SELECT object_key FROM material WHERE id=? AND workshop_id=?",id,workshop);enabled();
        try(var signer=S3Presigner.builder().region(Region.of(region)).build()) {
            var req=GetObjectRequest.builder().bucket(bucket).key(material.get("object_key").toString()).responseContentDisposition("attachment").build();
            return Map.of("downloadUrl",signer.presignGetObject(r->r.signatureDuration(Duration.ofMinutes(5)).getObjectRequest(req)).url().toString());
        }
    }
    private void enabled() {if(bucket.isBlank()) throw new ApiException(503,"Almacenamiento S3 no configurado");}
}
