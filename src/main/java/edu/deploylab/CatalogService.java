package edu.deploylab;

import java.util.*;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
    public record Template(String key,String title,String description,String evidence,String hint,String explanation,
                           String correctAction,String correctLabel,String wrongAction,String wrongLabel,String skill) {}
    public static final List<Template> TEMPLATES=List.of(
        new Template("API_URL","API mal configurada","La tienda no carga sus productos.",
            "GET /api/productos-viejo -> 404. El contrato de la API define GET /api/productos.",
            "Compara la ruta configurada con el contrato del backend.",
            "El cliente usaba una ruta inexistente. Corregir la URL restablece la petición; reiniciar no cambia la configuración.",
            "FIX_URL","Corregir URL del backend","RESTART","Reiniciar aplicación","Diagnóstico de APIs"),
        new Template("DB_AUTH","Conexión a base de datos","El backend no puede consultar el catálogo.",
            "SQLSTATE 28P01: authentication failed. Usuario configurado: app_old. Usuario autorizado: app_service.",
            "Revisa qué usuario utiliza la conexión. Todos los datos del escenario son ficticios.",
            "La conexión utilizaba credenciales incorrectas. Actualizar la configuración permite autenticar al servicio.",
            "FIX_CREDENTIALS","Corregir credenciales simuladas","INCREASE_TIMEOUT","Aumentar tiempo de espera","Bases de datos"),
        new Template("PERMISSIONS","Permisos insuficientes","Un instructor no puede consultar el panel de su grupo.",
            "GET /groups/dashboard -> 403. Sesión: STUDENT. Rol requerido y autorizado para este usuario ficticio: INSTRUCTOR.",
            "Compara el rol de la sesión simulada con el requerido por el recurso.",
            "Un 403 indica falta de autorización. Corregir el rol de la sesión ficticia resuelve el caso; nunca cambia tu rol real.",
            "FIX_ROLE","Corregir rol de la sesión simulada","CLEAR_CACHE","Limpiar caché","Autorización")
    );
    public record CreateWorkshop(@NotBlank @Size(max=150) String title,@NotBlank @Size(max=2000) String description,
        @NotBlank @Size(max=60) String topic,@Pattern(regexp="BEGINNER|INTERMEDIATE") @NotNull String difficulty,
        @NotEmpty @Size(max=3) List<@NotBlank String> templates) {}
    private final Db db;
    public CatalogService(Db db) {this.db=db;}
    public List<Map<String,Object>> list(String topic,String difficulty,String skill,int page,int size) {
        if(page<0 || page>10000 || size<1 || size>100) throw new ApiException(400,"Paginación inválida");
        return db.query("SELECT w.* FROM workshop w WHERE published=TRUE AND (?='' OR LOWER(topic)=LOWER(?)) AND (?='' OR difficulty=?) AND (?='' OR EXISTS (SELECT 1 FROM workshop_skill ws JOIN skill s ON s.id=ws.skill_id WHERE ws.workshop_id=w.id AND LOWER(s.name)=LOWER(?))) ORDER BY title,id LIMIT ? OFFSET ?",
            topic,topic,difficulty,difficulty,skill,skill,size,page*size);
    }
    public Map<String,Object> workshop(UUID id) {
        var w=db.one("SELECT * FROM workshop WHERE id=? AND published=TRUE",id);
        w.put("scenarios",db.query("SELECT id FROM scenario WHERE workshop_id=? ORDER BY title",id)
            .stream().map(row->scenario(Db.id(row,"id"))).toList());
        w.put("skills",db.query("SELECT s.* FROM skill s JOIN workshop_skill ws ON ws.skill_id=s.id WHERE ws.workshop_id=?",id));
        return w;
    }
    public Map<String,Object> scenario(UUID id) {
        var s=db.one("SELECT s.id,s.workshop_id,s.title,s.description,s.evidence FROM scenario s JOIN workshop w ON w.id=s.workshop_id WHERE s.id=? AND w.published=TRUE",id);
        s.put("actions",db.query("SELECT a.code,a.label FROM scenario_action a JOIN scenario_step p ON p.id=a.step_id WHERE p.scenario_id=? ORDER BY a.label",id));
        return s;
    }
    @Transactional
    public UUID create(AuthService.Actor actor,CreateWorkshop r) {
        actor.instructor();
        if(new HashSet<>(r.templates()).size()!=r.templates().size()) throw new ApiException(400,"Plantillas duplicadas");
        List<Template> templates=r.templates().stream().map(key->TEMPLATES.stream().filter(t->t.key().equals(key)).findFirst()
            .orElseThrow(()->new ApiException(400,"Plantilla desconocida"))).toList();
        UUID id=UUID.randomUUID();
        db.update("INSERT INTO workshop(id,title,description,topic,difficulty,owner_id) VALUES (?,?,?,?,?,?)",id,r.title(),r.description(),r.topic(),r.difficulty(),actor.id());
        templates.forEach(t->addScenario(id,t));
        return id;
    }
    void addScenario(UUID workshopId,Template t) {
        UUID sid=UUID.randomUUID(),step=UUID.randomUUID();
        UUID skill=Db.id(db.one("SELECT id FROM skill WHERE name=?",t.skill()),"id");
        db.update("INSERT INTO scenario(id,workshop_id,title,description,template_key,evidence,hint,explanation,skill_id) VALUES (?,?,?,?,?,?,?,?,?)",
            sid,workshopId,t.title(),t.description(),t.key(),t.evidence(),t.hint(),t.explanation(),skill);
        db.update("INSERT INTO scenario_step(id,scenario_id,position,title) VALUES (?,?,1,'Diagnosticar y corregir')",step,sid);
        db.update("INSERT INTO scenario_action(id,step_id,code,label,correct) VALUES (?,?,?,?,TRUE)",UUID.randomUUID(),step,t.correctAction(),t.correctLabel());
        db.update("INSERT INTO scenario_action(id,step_id,code,label,correct) VALUES (?,?,?,?,FALSE)",UUID.randomUUID(),step,t.wrongAction(),t.wrongLabel());
        db.update("INSERT INTO workshop_skill(workshop_id,skill_id) VALUES (?,?)",workshopId,skill);
    }
    public void requireOwner(UUID workshop,AuthService.Actor actor) {
        actor.instructor(); var w=db.one("SELECT owner_id FROM workshop WHERE id=?",workshop);
        if(!actor.id().equals(w.get("owner_id"))) throw ApiException.forbidden();
    }
}
