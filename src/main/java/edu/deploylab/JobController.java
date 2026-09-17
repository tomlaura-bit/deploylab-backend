package edu.deploylab;

import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api")
public class JobController {
    private final JobService jobs;private final Db db;
    public JobController(JobService jobs,Db db) {this.jobs=jobs;this.db=db;}
    @PostMapping("/reports") @ResponseStatus(HttpStatus.ACCEPTED)
    public Object request(@AuthenticationPrincipal AuthService.Actor user) {return Map.of("id",jobs.enqueue(user.id(),"PERSONAL_REPORT",""));}
    @GetMapping("/jobs/{id}") public Object status(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user) {return jobs.status(id,user);}
    @GetMapping("/jobs/{id}/download") public ResponseEntity<String> download(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=deploylab-report.csv")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(jobs.download(id,user));
    }
    @GetMapping("/notifications") public Object notifications(@AuthenticationPrincipal AuthService.Actor user) {
        return db.jdbc.queryForList("SELECT * FROM notification WHERE user_id=? ORDER BY created_at DESC LIMIT 100",user.id());
    }
    @PatchMapping("/notifications/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@PathVariable UUID id,@AuthenticationPrincipal AuthService.Actor user) {
        if(db.jdbc.update("UPDATE notification SET read_at=CURRENT_TIMESTAMP WHERE id=? AND user_id=?",id,user.id())==0) throw ApiException.missing();
    }
}
