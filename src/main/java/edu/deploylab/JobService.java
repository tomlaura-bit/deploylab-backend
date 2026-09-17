package edu.deploylab;

import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class JobService {
    private final Db db;
    public JobService(Db db) {this.db=db;}
    public UUID enqueue(UUID owner,String kind,String payload) {
        UUID id=UUID.randomUUID();
        db.jdbc.update("INSERT INTO background_job(id,owner_id,kind,state,payload) VALUES (?,?,?,'PENDING',?)",id,owner,kind,payload);
        return id;
    }
    public Map<String,Object> owned(UUID id,AuthService.Actor user) {return db.one("SELECT * FROM background_job WHERE id=? AND owner_id=?",id,user.id());}
    public Object status(UUID id,AuthService.Actor user) {
        var job=owned(id,user);job.remove("payload");job.remove("result");return job;
    }
    public String download(UUID id,AuthService.Actor user) {
        var job=owned(id,user);
        if(!job.get("kind").toString().endsWith("REPORT")) throw new ApiException(400,"Este trabajo no genera un archivo");
        if(!job.get("state").equals("SUCCEEDED")) throw new ApiException(409,"El reporte aún no está disponible");
        return job.get("result").toString();
    }
}
