package edu.deploylab;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttemptService {
    private final Db db; private final CatalogService catalog; private final SimulationEngine engine; private final ApplicationEventPublisher events;
    public AttemptService(Db db,CatalogService catalog,SimulationEngine engine,ApplicationEventPublisher events) {this.db=db;this.catalog=catalog;this.engine=engine;this.events=events;}
    @Transactional public UUID start(AuthService.Actor user,UUID scenario) {
        catalog.scenario(scenario); UUID id=UUID.randomUUID();
        db.update("INSERT INTO attempt(id,user_id,scenario_id,state) VALUES (?,?,?,'IN_PROGRESS')",id,user.id(),scenario);
        return id;
    }
    public Map<String,Object> owned(UUID id,AuthService.Actor user,boolean lock) {
        // Scope by owner so another user's resource is indistinguishable from an absent one.
        return db.one("SELECT * FROM attempt WHERE id=? AND user_id=?"+(lock?" FOR UPDATE":""),id,user.id());
    }
    @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Object detail(UUID id,AuthService.Actor user) {
        var a=owned(id,user,false);
        a.put("events",db.query("SELECT id,event_index,action_code,result_state,score,feedback,created_at FROM attempt_event WHERE attempt_id=? ORDER BY event_index",id));
        if(Boolean.TRUE.equals(a.get("finalized"))) {
            var evaluation=db.one("SELECT * FROM attempt_evaluation WHERE attempt_id=?",id);
            a.put("evaluation",evaluation);
            a.put("explanation",evaluation.get("explanation"));
        }
        return a;
    }
    public Object list(AuthService.Actor user,int page,int size) {
        if(page<0 || page>10000 || size<1 || size>100) throw new ApiException(400,"Paginación inválida");
        return db.query("SELECT * FROM attempt WHERE user_id=? ORDER BY created_at DESC,id LIMIT ? OFFSET ?",user.id(),size,page*size);
    }
    @Transactional public Map<String,Object> action(UUID id,AuthService.Actor user,UUID key,String code) {
        var a=owned(id,user,true);
        var previous=db.query("SELECT * FROM attempt_event WHERE attempt_id=? AND request_key=?",id,key);
        if(!previous.isEmpty()) {
            if(!previous.getFirst().get("action_code").equals(code)) throw new ApiException(409,"La clave de idempotencia ya fue utilizada con otra acción");
            return previous.getFirst();
        }
        if(Boolean.TRUE.equals(a.get("finalized"))) throw new ApiException(409,"El intento ya fue finalizado");
        var actions=db.query("SELECT a.code,a.correct FROM scenario_action a JOIN scenario_step p ON p.id=a.step_id WHERE p.scenario_id=?",a.get("scenario_id"));
        if(actions.stream().noneMatch(x->x.get("code").equals(code))) throw new ApiException(400,"Acción no disponible para este escenario");
        String correct=actions.stream().filter(x->Boolean.TRUE.equals(x.get("correct"))).findFirst().orElseThrow().get("code").toString();
        var result=engine.apply(a.get("state").toString(),code,correct,((Number)a.get("mistakes")).intValue(),((Number)a.get("hints")).intValue());
        boolean solved=result.state().equals("RESOLVED");
        db.update("UPDATE attempt SET state=?,mistakes=mistakes+? WHERE id=?",result.state(),solved?0:1,id);
        UUID event=UUID.randomUUID();
        int index=db.scalar("SELECT COALESCE(MAX(event_index),0)+1 FROM attempt_event WHERE attempt_id=?",Integer.class,id);
        db.update("INSERT INTO attempt_event(id,attempt_id,request_key,action_code,result_state,score,feedback,event_index) VALUES (?,?,?,?,?,?,?,?)",event,id,key,code,result.state(),result.score(),result.feedback(),index);
        return db.one("SELECT * FROM attempt_event WHERE id=?",event);
    }
    @Transactional public Object finish(UUID id,AuthService.Actor user) {
        var a=owned(id,user,true);
        // A row lock serializes finalization with both other finalizations and actions.
        if(Boolean.TRUE.equals(a.get("finalized"))) return detail(id,user);
        if(!db.exists("SELECT id FROM attempt_event WHERE attempt_id=?",id))
            throw new ApiException(409,"Registra al menos una acción antes de finalizar");
        int mistakes=((Number)a.get("mistakes")).intValue(),hints=((Number)a.get("hints")).intValue();
        boolean solved="RESOLVED".equals(a.get("state"));
        int score=engine.evaluate(a.get("state").toString(),mistakes,hints);
        String explanation=db.one("SELECT explanation FROM scenario WHERE id=?",a.get("scenario_id")).get("explanation").toString();
        db.update("INSERT INTO attempt_evaluation(attempt_id,score,mistakes,hints,solved,explanation) VALUES (?,?,?,?,?,?)",id,score,mistakes,hints,solved,explanation);
        db.update("UPDATE attempt SET finalized=TRUE,score=?,completed_at=CURRENT_TIMESTAMP WHERE id=?",score,id);
        events.publishEvent(new AttemptFinishedEvent(id,user.id(),score,solved,java.time.Instant.now()));
        return detail(id,user);
    }
    @Transactional public Object hint(UUID id,AuthService.Actor user) {
        var a=owned(id,user,true);
        if(Boolean.TRUE.equals(a.get("finalized")) || !a.get("state").equals("IN_PROGRESS")) throw new ApiException(409,"El intento ya está resuelto o finalizado");
        // One hint per scenario: repeat reads do not penalize twice.
        db.update("UPDATE attempt SET hints=1 WHERE id=?",id);
        return db.one("SELECT hint FROM scenario WHERE id=?",a.get("scenario_id"));
    }
    public Object progress(AuthService.Actor user) {
        return db.query("SELECT s.id,s.name,COUNT(a.id) AS resolved_attempts,COALESCE(MAX(a.score),0) AS best_score FROM skill s JOIN scenario sc ON sc.skill_id=s.id LEFT JOIN attempt a ON a.scenario_id=sc.id AND a.user_id=? AND a.state='RESOLVED' AND a.finalized=TRUE GROUP BY s.id,s.name ORDER BY s.name",user.id());
    }
}
