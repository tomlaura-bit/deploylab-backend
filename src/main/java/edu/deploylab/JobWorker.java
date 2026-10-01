package edu.deploylab;

import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.extras-enabled",havingValue="true")
public class JobWorker {
    private final Db db; private final JavaMailSender mail;
    @Value("${app.mail-enabled}") boolean mailEnabled;
    @Value("${app.mail-from}") String from;
    public JobWorker(Db db,JavaMailSender mail) {this.db=db;this.mail=mail;}
    @Scheduled(fixedDelayString="${app.jobs-delay}",initialDelayString="${app.jobs-delay}")
    public void tick() {
        db.update("DELETE FROM auth_token WHERE expires_at<CURRENT_TIMESTAMP");
        // Recover work abandoned by a stopped process. Mail delivery is at-least-once.
        db.update("UPDATE background_job SET state=CASE WHEN attempts>=3 THEN 'FAILED' ELSE 'PENDING' END,error='Worker interrumpido',updated_at=CURRENT_TIMESTAMP WHERE state='RUNNING' AND updated_at<?",OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(10));
        for(var job:db.query("SELECT * FROM background_job WHERE state='PENDING' ORDER BY created_at LIMIT 10")) {
            UUID id=Db.id(job,"id");
            if(db.update("UPDATE background_job SET state='RUNNING',attempts=attempts+1,updated_at=CURRENT_TIMESTAMP WHERE id=? AND state='PENDING'",id)!=1) continue;
            try {
                String result=process(job);
                db.update("UPDATE background_job SET state='SUCCEEDED',result=?,error=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=?",result,id);
            } catch(Exception e) {
                // Do not persist provider exceptions: they can contain credentials or recipient details.
                db.update("UPDATE background_job SET state=CASE WHEN attempts>=3 THEN 'FAILED' ELSE 'PENDING' END,error='No se pudo completar el servicio',updated_at=CURRENT_TIMESTAMP WHERE id=?",id);
            }
        }
    }
    String process(Map<String,Object> job) {
        String kind=job.get("kind").toString(); UUID owner=Db.id(job,"owner_id");
        if(kind.equals("EMAIL")) {
            if(!mailEnabled) throw new IllegalStateException("SMTP no habilitado");
            var user=db.one("SELECT email FROM app_user WHERE id=?",owner);
            var message=new SimpleMailMessage(); message.setFrom(from); message.setTo(user.get("email").toString());
            message.setSubject("DeployLab: nuevo taller");message.setText(job.get("payload").toString());mail.send(message);
            return "Entregado al servidor SMTP";
        }
        List<Map<String,Object>> rows;
        if(kind.equals("PERSONAL_REPORT")) {
            rows=db.query("SELECT sc.title,a.state,a.finalized,a.score,a.mistakes,a.hints,a.created_at FROM attempt a JOIN scenario sc ON sc.id=a.scenario_id WHERE a.user_id=? ORDER BY a.created_at",owner);
        } else if(kind.equals("GROUP_REPORT")) {
            UUID group=UUID.fromString(job.get("payload").toString());
            db.one("SELECT id FROM study_group WHERE id=? AND instructor_id=?",group,owner);
            rows=db.query("SELECT u.name,sc.title,a.state,a.score,a.mistakes,a.hints FROM membership m JOIN app_user u ON u.id=m.user_id JOIN attempt a ON a.user_id=u.id JOIN scenario sc ON sc.id=a.scenario_id JOIN assignment ass ON ass.id=a.assignment_id AND ass.group_id=m.group_id WHERE m.group_id=? AND m.role='STUDENT' AND a.finalized=TRUE ORDER BY u.name,a.created_at",group);
        } else throw new IllegalArgumentException("Tipo desconocido");
        if(rows.isEmpty()) return "resultado\r\n\"Sin intentos registrados\"\r\n";
        var columns=new ArrayList<>(rows.getFirst().keySet()); StringBuilder csv=new StringBuilder(String.join(",",columns)).append("\r\n");
        for(var row:rows) {
            csv.append(String.join(",",columns.stream().map(c->escapeCsv(row.get(c))).toList())).append("\r\n");
        }
        return csv.toString();
    }
    static String escapeCsv(Object value) {
        String s=Objects.toString(value,"");
        if(s.matches("^[\\s]*[=+@-].*") || s.startsWith("\t") || s.startsWith("\r")) s="'"+s;
        return "\""+s.replace("\"","\"\"")+"\"";
    }
}
