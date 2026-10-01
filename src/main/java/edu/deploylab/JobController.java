package edu.deploylab;

import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.extras-enabled",havingValue="true")
public class JobController {
    private final JobService jobs;private final Db db;
    public JobController(JobService jobs,Db db) {this.jobs=jobs;this.db=db;}
    @GetMapping("/jobs") public Object list(@AuthenticationPrincipal AuthService.Actor user,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        AssignmentService.page(page,size);
        return db.query("SELECT id,kind,state,attempts,error,created_at,updated_at FROM background_job WHERE owner_id=? ORDER BY created_at DESC,id LIMIT ? OFFSET ?",user.id(),size,page*size);
    }
    @PostMapping("/jobs/{id}/retry") @ResponseStatus(HttpStatus.ACCEPTED)
    public Object retry(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user) {
        jobs.owned(id,user);
        if(db.update("UPDATE background_job SET state='PENDING',attempts=0,error=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=? AND owner_id=? AND state='FAILED'",id,user.id())==0) throw new ApiException(409,"Solo se pueden reintentar trabajos fallidos");
        return Map.of("id",id);
    }
    @PatchMapping("/notifications/read-all") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readAll(@AuthenticationPrincipal AuthService.Actor user) {db.update("UPDATE notification SET read_at=CURRENT_TIMESTAMP WHERE user_id=? AND read_at IS NULL",user.id());}
    @PostMapping("/reports") @ResponseStatus(HttpStatus.ACCEPTED)
    public Object request(@AuthenticationPrincipal AuthService.Actor user) {return Map.of("id",jobs.enqueue(user.id(),"PERSONAL_REPORT",""));}
    @GetMapping("/jobs/{id}") public Object status(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user) {return jobs.status(id,user);}
    @GetMapping("/jobs/{id}/download") public ResponseEntity<String> download(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=deploylab-report.csv")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(jobs.download(id,user));
    }
    @GetMapping("/notifications") public Object notifications(@AuthenticationPrincipal AuthService.Actor user,@RequestParam(defaultValue="false") boolean unreadOnly,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        AssignmentService.page(page,size);
        return db.query("SELECT * FROM notification WHERE user_id=? AND (?=FALSE OR read_at IS NULL) ORDER BY created_at DESC,id LIMIT ? OFFSET ?",user.id(),unreadOnly,size,page*size);
    }
    @PatchMapping("/notifications/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user) {
        if(db.update("UPDATE notification SET read_at=CURRENT_TIMESTAMP WHERE id=? AND user_id=?",id,user.id())==0) throw ApiException.missing();
    }
}
