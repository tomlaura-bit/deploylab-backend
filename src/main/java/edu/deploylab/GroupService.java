package edu.deploylab;

import java.time.OffsetDateTime;
import java.util.*;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupService {
    public record NewGroup(@NotBlank @Size(max=120) String name) {}
    public record Member(@NotNull UUID userId) {}
    public record Assign(@NotNull UUID workshopId,@NotNull @Future OffsetDateTime dueAt) {}
    private final Db db; private final JobService jobs; private final CatalogService catalog;
    public GroupService(Db db,JobService jobs,CatalogService catalog) {this.db=db;this.jobs=jobs;this.catalog=catalog;}
    public Map<String,Object> owner(UUID group,AuthService.Actor user) {
        user.instructor(); return db.one("SELECT * FROM study_group WHERE id=? AND instructor_id=?",group,user.id());
    }
    public void member(UUID group,AuthService.Actor user) {
        if(!db.exists("SELECT 1 FROM membership WHERE group_id=? AND user_id=?",group,user.id())) throw ApiException.missing();
    }
    @Transactional public UUID create(AuthService.Actor user,String name) {
        user.instructor(); UUID id=UUID.randomUUID();
        db.jdbc.update("INSERT INTO study_group(id,name,instructor_id) VALUES (?,?,?)",id,name,user.id());
        db.jdbc.update("INSERT INTO membership(group_id,user_id,role) VALUES (?,?,'INSTRUCTOR')",id,user.id());
        return id;
    }
    public Object list(AuthService.Actor user) {
        return db.jdbc.queryForList("SELECT g.* FROM study_group g JOIN membership m ON m.group_id=g.id WHERE m.user_id=? ORDER BY g.name",user.id());
    }
    public Object detail(UUID group,AuthService.Actor user) {
        member(group,user);
        var g=db.one("SELECT * FROM study_group WHERE id=?",group);
        g.put("assignments",db.jdbc.queryForList("SELECT a.*,w.title FROM assignment a JOIN workshop w ON w.id=a.workshop_id WHERE group_id=? ORDER BY due_at",group));
        if(user.id().equals(g.get("instructor_id"))) g.put("members",db.jdbc.queryForList("SELECT u.id,u.name,u.email,m.role FROM membership m JOIN app_user u ON u.id=m.user_id WHERE m.group_id=?",group));
        return g;
    }
    @Transactional public void add(UUID group,AuthService.Actor user,UUID member) {
        activeOwner(group,user);
        var u=db.one("SELECT role FROM app_user WHERE id=?",member);
        if(!u.get("role").equals("STUDENT")) throw new ApiException(400,"Solo puedes añadir estudiantes");
        db.jdbc.update("INSERT INTO membership(group_id,user_id,role) VALUES (?,?,'STUDENT')",group,member);
    }
    @Transactional public void remove(UUID group,AuthService.Actor user,UUID member) {
        owner(group,user);
        if(user.id().equals(member)) throw new ApiException(409,"No puedes eliminar al instructor propietario");
        if(db.jdbc.update("DELETE FROM membership WHERE group_id=? AND user_id=?",group,member)==0) throw ApiException.missing();
    }
    @Transactional public UUID assign(UUID group,AuthService.Actor user,Assign r) {
        activeOwner(group,user); catalog.workshop(r.workshopId());
        UUID id=UUID.randomUUID();
        db.jdbc.update("INSERT INTO assignment(id,group_id,workshop_id,due_at) VALUES (?,?,?,?)",id,group,r.workshopId(),r.dueAt());
        for(var m:db.jdbc.queryForList("SELECT user_id FROM membership WHERE group_id=? AND role='STUDENT'",group)) {
            UUID uid=Db.id(m,"user_id");
            String message="Tienes un nuevo taller asignado. Fecha límite: "+r.dueAt();
            db.jdbc.update("INSERT INTO notification(id,user_id,message) VALUES (?,?,?)",UUID.randomUUID(),uid,message);
            jobs.enqueue(uid,"EMAIL",message);
        }
        return id;
    }
    public record EditGroup(@NotBlank @Size(max=120) String name,@NotNull Boolean archived) {}
    public record EmailMember(@NotBlank @Email @Size(max=254) String email) {}
    private void activeOwner(UUID group,AuthService.Actor user) {
        var g=owner(group,user);
        if(Boolean.TRUE.equals(g.get("archived"))) throw new ApiException(409,"El grupo está archivado");
    }
    @Transactional public void edit(UUID group,AuthService.Actor user,EditGroup body) {
        owner(group,user);db.jdbc.update("UPDATE study_group SET name=?,archived=? WHERE id=?",body.name(),body.archived(),group);
    }
    @Transactional public void addByEmail(UUID group,AuthService.Actor user,String email) {
        activeOwner(group,user);
        var member=db.one("SELECT id FROM app_user WHERE email=?",email.trim().toLowerCase(java.util.Locale.ROOT));
        add(group,user,Db.id(member,"id"));
    }
    public Object statistics(UUID group,AuthService.Actor user) {
        owner(group,user);
        return db.jdbc.queryForList("SELECT u.id,u.name,COUNT(a.id) AS attempts,COALESCE(SUM(CASE WHEN a.finalized=TRUE AND a.state='RESOLVED' THEN 1 ELSE 0 END),0) AS resolved,COALESCE(MAX(CASE WHEN a.finalized=TRUE THEN a.score END),0) AS best_score FROM membership m JOIN app_user u ON u.id=m.user_id LEFT JOIN attempt a ON a.user_id=u.id AND a.assignment_id IN (SELECT ass.id FROM assignment ass WHERE ass.group_id=?) WHERE m.group_id=? AND m.role='STUDENT' GROUP BY u.id,u.name ORDER BY u.name",group,group);
    }
}
