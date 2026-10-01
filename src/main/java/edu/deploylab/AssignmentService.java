package edu.deploylab;

import java.util.*;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignmentService {
    private final Db db; private final GroupService groups; private final AttemptService attempts;
    public AssignmentService(Db db,GroupService groups,AttemptService attempts) {this.db=db;this.groups=groups;this.attempts=attempts;}
    public static void page(int page,int size) {
        if(page<0 || page>10000 || size<1 || size>100) throw new ApiException(400,"Paginación inválida");
    }
    public Object list(AuthService.Actor user,int page,int size) {
        page(page,size);
        return db.query("SELECT a.*,w.title,g.name AS group_name,g.archived FROM assignment a JOIN workshop w ON w.id=a.workshop_id JOIN study_group g ON g.id=a.group_id JOIN membership m ON m.group_id=g.id WHERE m.user_id=? ORDER BY a.due_at,a.id LIMIT ? OFFSET ?",user.id(),size,page*size);
    }
    public Map<String,Object> accessible(UUID id,AuthService.Actor user) {
        var a=db.one("SELECT a.*,g.instructor_id,g.archived FROM assignment a JOIN study_group g ON g.id=a.group_id WHERE a.id=?",id);
        groups.member(Db.id(a,"group_id"),user); return a;
    }
    public Object detail(UUID id,AuthService.Actor user) {
        var a=accessible(id,user);
        a.put("scenarios",db.query("SELECT s.id,s.title,(SELECT COUNT(*) FROM attempt t WHERE t.assignment_id=? AND t.scenario_id=s.id AND t.user_id=? AND t.finalized=TRUE) AS submissions FROM scenario s WHERE s.workshop_id=? ORDER BY s.title",id,user.id(),a.get("workshop_id")));
        a.put("myAttempts",db.query("SELECT id,scenario_id,state,finalized,score,created_at,completed_at FROM attempt WHERE assignment_id=? AND user_id=? ORDER BY created_at,id",id,user.id()));
        return a;
    }
    @Transactional public UUID start(UUID id,AuthService.Actor user,UUID scenario) {
        if(!user.role().equals("STUDENT")) throw ApiException.forbidden();
        var a=db.one("SELECT * FROM assignment WHERE id=? FOR UPDATE",id);
        var g=db.one("SELECT * FROM study_group WHERE id=? FOR UPDATE",a.get("group_id"));
        groups.member(Db.id(a,"group_id"),user);
        if(Boolean.TRUE.equals(a.get("cancelled")) || Boolean.TRUE.equals(g.get("archived"))) throw new ApiException(409,"La asignación está cerrada");
        if(db.exists("SELECT 1 FROM assignment WHERE id=? AND due_at<CURRENT_TIMESTAMP",id)) throw new ExpiredAssignmentException();
        if(!db.exists("SELECT 1 FROM scenario WHERE id=? AND workshop_id=?",scenario,a.get("workshop_id"))) throw new ApiException(400,"El escenario no pertenece al taller asignado");
        UUID attempt=attempts.start(user,scenario);
        db.update("UPDATE attempt SET assignment_id=? WHERE id=?",id,attempt); return attempt;
    }
    @Transactional public void update(UUID id,AuthService.Actor user,OffsetDateTime dueAt) {
        var a=db.one("SELECT * FROM assignment WHERE id=? FOR UPDATE",id); groups.owner(Db.id(a,"group_id"),user);
        if(Boolean.TRUE.equals(a.get("cancelled"))) throw new ApiException(409,"La asignación está cancelada");
        db.update("UPDATE assignment SET due_at=? WHERE id=?",dueAt,id);
    }
    @Transactional public void cancel(UUID id,AuthService.Actor user) {
        var a=db.one("SELECT * FROM assignment WHERE id=? FOR UPDATE",id);groups.owner(Db.id(a,"group_id"),user);
        db.update("UPDATE assignment SET cancelled=TRUE WHERE id=?",id);
    }
    public Object submissions(UUID id,AuthService.Actor user,int page,int size) {
        page(page,size);var a=accessible(id,user);groups.owner(Db.id(a,"group_id"),user);
        return db.query("SELECT t.id,t.user_id,u.name,t.scenario_id,t.state,t.score,t.completed_at,(t.completed_at>a.due_at) AS late FROM attempt t JOIN app_user u ON u.id=t.user_id JOIN assignment a ON a.id=t.assignment_id WHERE t.assignment_id=? AND t.finalized=TRUE ORDER BY t.completed_at DESC,t.id LIMIT ? OFFSET ?",id,size,page*size);
    }
    public Object submission(UUID id,UUID attempt,AuthService.Actor user) {
        var a=accessible(id,user);groups.owner(Db.id(a,"group_id"),user);
        var t=db.one("SELECT * FROM attempt WHERE id=? AND assignment_id=? AND finalized=TRUE",attempt,id);
        t.put("evaluation",db.one("SELECT * FROM attempt_evaluation WHERE attempt_id=?",attempt));
        t.put("events",db.query("SELECT * FROM attempt_event WHERE attempt_id=? ORDER BY event_index",attempt));return t;
    }
}
