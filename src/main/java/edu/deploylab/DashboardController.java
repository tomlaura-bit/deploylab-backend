package edu.deploylab;

import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.extras-enabled",havingValue="true")
public class DashboardController {
    private final Db db;private final AttemptService attempts;
    public DashboardController(Db db,AttemptService attempts) {this.db=db;this.attempts=attempts;}
    @GetMapping("/api/dashboard") public Object dashboard(@AuthenticationPrincipal AuthService.Actor user) {
        var result=new LinkedHashMap<String,Object>(); result.put("role",user.role());
        result.put("practice",db.one("SELECT COUNT(*) AS attempts,COALESCE(SUM(CASE WHEN finalized=TRUE THEN 1 ELSE 0 END),0) AS finalized,COALESCE(SUM(CASE WHEN finalized=TRUE AND state='RESOLVED' THEN 1 ELSE 0 END),0) AS solved,COALESCE(MAX(CASE WHEN finalized=TRUE THEN score END),0) AS best_score FROM attempt WHERE user_id=?",user.id()));
        result.put("unreadNotifications",db.scalar("SELECT COUNT(*) FROM notification WHERE user_id=? AND read_at IS NULL",Long.class,user.id()));
        result.put("skills",attempts.progress(user));
        if(user.role().equals("INSTRUCTOR")) {
            result.put("groups",db.scalar("SELECT COUNT(*) FROM study_group WHERE instructor_id=? AND archived=FALSE",Long.class,user.id()));
            result.put("workshops",db.scalar("SELECT COUNT(*) FROM workshop WHERE owner_id=?",Long.class,user.id()));
            result.put("submissions",db.scalar("SELECT COUNT(*) FROM attempt t JOIN assignment a ON a.id=t.assignment_id JOIN study_group g ON g.id=a.group_id WHERE g.instructor_id=? AND t.finalized=TRUE",Long.class,user.id()));
        } else {
            result.put("openAssignments",db.scalar("SELECT COUNT(*) FROM assignment a JOIN membership m ON m.group_id=a.group_id JOIN study_group g ON g.id=a.group_id WHERE m.user_id=? AND a.cancelled=FALSE AND g.archived=FALSE AND a.due_at>=CURRENT_TIMESTAMP",Long.class,user.id()));
        }
        return result;
    }
}
