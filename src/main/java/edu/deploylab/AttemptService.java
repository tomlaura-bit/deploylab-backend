package edu.deploylab;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttemptService {
    private final Db db; private final CatalogService catalog; private final SimulationEngine engine;
    public AttemptService(Db db,CatalogService catalog,SimulationEngine engine) {this.db=db;this.catalog=catalog;this.engine=engine;}
    public UUID start(AuthService.Actor user,UUID scenario) {
        catalog.scenario(scenario); UUID id=UUID.randomUUID();
        db.jdbc.update("INSERT INTO attempt(id,user_id,scenario_id,state) VALUES (?,?,?,'IN_PROGRESS')",id,user.id(),scenario);
        return id;
    }
    public Map<String,Object> owned(UUID id,AuthService.Actor user,boolean lock) {
        // Scope by owner so another user's resource is indistinguishable from an absent one.
        return db.one("SELECT * FROM attempt WHERE id=? AND user_id=?"+(lock?" FOR UPDATE":""),id,user.id());
    }
    public Object detail(UUID id,AuthService.Actor user) {
        var a=owned(id,user,false);
        a.put("events",db.jdbc.queryForList("SELECT action_code,result_state,score,feedback,created_at FROM attempt_event WHERE attempt_id=? ORDER BY created_at,id",id));
        if(a.get("state").equals("RESOLVED")) a.put("explanation",db.one("SELECT explanation FROM scenario WHERE id=?",a.get("scenario_id")).get("explanation"));
        return a;
    }
    public Object list(AuthService.Actor user,int page,int size) {
        if(page<0 || page>10000 || size<1 || size>100) throw new ApiException(400,"Paginación inválida");
        return db.jdbc.queryForList("SELECT * FROM attempt WHERE user_id=? ORDER BY created_at DESC,id LIMIT ? OFFSET ?",user.id(),size,page*size);
    }
    @Transactional public Map<String,Object> action(UUID id,AuthService.Actor user,UUID key,String code) {
        var a=owned(id,user,true);
        var previous=db.jdbc.queryForList("SELECT * FROM attempt_event WHERE attempt_id=? AND request_key=?",id,key);
        if(!previous.isEmpty()) {
            if(!previous.getFirst().get("action_code").equals(code)) throw new ApiException(409,"La clave de idempotencia ya fue utilizada con otra acción");
            return previous.getFirst();
        }
        var actions=db.jdbc.queryForList("SELECT a.code,a.correct FROM scenario_action a JOIN scenario_step p ON p.id=a.step_id WHERE p.scenario_id=?",a.get("scenario_id"));
        if(actions.stream().noneMatch(x->x.get("code").equals(code))) throw new ApiException(400,"Acción no disponible para este escenario");
        String correct=actions.stream().filter(x->Boolean.TRUE.equals(x.get("correct"))).findFirst().orElseThrow().get("code").toString();
        var result=engine.apply(a.get("state").toString(),code,correct,((Number)a.get("mistakes")).intValue(),((Number)a.get("hints")).intValue());
        boolean solved=result.state().equals("RESOLVED");
        db.jdbc.update("UPDATE attempt SET state=?,score=?,mistakes=mistakes+?,completed_at=CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE completed_at END WHERE id=?",result.state(),result.score(),solved?0:1,solved,id);
        UUID event=UUID.randomUUID();
        db.jdbc.update("INSERT INTO attempt_event(id,attempt_id,request_key,action_code,result_state,score,feedback) VALUES (?,?,?,?,?,?,?)",event,id,key,code,result.state(),result.score(),result.feedback());
        return db.one("SELECT * FROM attempt_event WHERE id=?",event);
    }
    @Transactional public Object hint(UUID id,AuthService.Actor user) {
        var a=owned(id,user,true);
        if(!a.get("state").equals("IN_PROGRESS")) throw new ApiException(409,"El intento ya está resuelto");
        // One hint per scenario: repeat reads do not penalize twice.
        db.jdbc.update("UPDATE attempt SET hints=1 WHERE id=?",id);
        return db.one("SELECT hint FROM scenario WHERE id=?",a.get("scenario_id"));
    }
    public Object progress(AuthService.Actor user) {
        return db.jdbc.queryForList("SELECT s.id,s.name,COUNT(a.id) AS resolved_attempts,COALESCE(MAX(a.score),0) AS best_score FROM skill s JOIN workshop_skill ws ON ws.skill_id=s.id JOIN scenario sc ON sc.workshop_id=ws.workshop_id LEFT JOIN attempt a ON a.scenario_id=sc.id AND a.user_id=? AND a.state='RESOLVED' GROUP BY s.id,s.name ORDER BY s.name",user.id());
    }
}
