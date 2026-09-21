package edu.deploylab;

import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartFile;

@RestController @RequestMapping("/api/workshops/{workshop}/materials")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.extras-enabled",havingValue="true")
public class MaterialController {
    public record Upload(@NotBlank @Pattern(regexp="[a-zA-Z0-9_-]{1,100}\\.pdf") String filename) {}
    private final Db db;private final CatalogService catalog;private final S3Storage storage;
    public MaterialController(Db db,CatalogService catalog,S3Storage storage) {this.db=db;this.catalog=catalog;this.storage=storage;}
    @GetMapping public Object list(@PathVariable UUID workshop) {catalog.workshop(workshop);return db.jdbc.queryForList("SELECT id,filename FROM material WHERE workshop_id=? AND status='READY' AND deleted=FALSE",workshop);}
    @PostMapping(value="/local",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public Object local(@PathVariable UUID workshop,@AuthenticationPrincipal AuthService.Actor user,@RequestPart("file") MultipartFile file) throws java.io.IOException {
        catalog.requireOwner(workshop,user);
        String filename=file.getOriginalFilename();
        if(filename==null || !filename.matches("[a-zA-Z0-9_-]{1,100}\\.pdf")) throw new ApiException(400,"Nombre PDF inválido");
        if(file.isEmpty() || file.getSize()>10*1024*1024) throw new ApiException(400,"El PDF debe medir entre 1 byte y 10 MiB");
        byte[] content=file.getBytes();
        if(content.length<5 || !new String(content,0,5,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-")) throw new ApiException(400,"El archivo no tiene una cabecera PDF válida");
        UUID id=UUID.randomUUID();
        db.jdbc.update("INSERT INTO material(id,workshop_id,filename,object_key,status,content) VALUES (?,?,?,?,'READY',?)",id,workshop,filename,"local/"+id,content);
        return Map.of("id",id,"filename",filename);
    }
    @GetMapping("/{id}/content") public ResponseEntity<byte[]> content(@PathVariable UUID workshop,@PathVariable UUID id) {
        catalog.workshop(workshop);
        var m=db.one("SELECT filename,content FROM material WHERE id=? AND workshop_id=? AND status='READY' AND deleted=FALSE AND content IS NOT NULL",id,workshop);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+m.get("filename")+"\"")
            .header("X-Content-Type-Options","nosniff").header(HttpHeaders.CACHE_CONTROL,"private, no-store").body((byte[])m.get("content"));
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID workshop,@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user) {
        catalog.requireOwner(workshop,user);
        if(db.jdbc.update("UPDATE material SET deleted=TRUE,content=NULL WHERE id=? AND workshop_id=?",id,workshop)==0) throw ApiException.missing();
    }
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
        var material=db.one("SELECT object_key FROM material WHERE id=? AND workshop_id=? AND deleted=FALSE AND content IS NULL",id,workshop);
        storage.verifyUpload(material.get("object_key").toString());
        db.jdbc.update("UPDATE material SET status='READY' WHERE id=?",id);
    }
    @GetMapping("/{id}/download") public Object download(@PathVariable UUID workshop,@PathVariable UUID id) {
        catalog.workshop(workshop);var material=db.one("SELECT object_key,content FROM material WHERE id=? AND workshop_id=? AND status='READY' AND deleted=FALSE",id,workshop);
        if(material.get("content")!=null) return Map.of("downloadUrl","/api/workshops/"+workshop+"/materials/"+id+"/content");
        return Map.of("downloadUrl",storage.downloadUrl(material.get("object_key").toString()));
    }
}
