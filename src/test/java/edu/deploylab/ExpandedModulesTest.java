package edu.deploylab;

import java.util.*;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.assertj.core.api.Assertions.*;

class ExpandedModulesTest extends ApiTestSupport {
    @Test void deadlinesMembershipAndArchivingRestrictNewAttempts() throws Exception {
        var t=teacher();var s=student();var outsider=student();var g=group(t);
        String email=db.one("SELECT email FROM app_user WHERE id=?",s.id()).get("email").toString();
        perform(as(body(post("/api/groups/{id}/members/by-email",g),Map.of("email",email)),t),204);
        String a=perform(as(body(post("/api/groups/{id}/assignments",g),Map.of("workshopId","10000000-0000-0000-0000-000000000001","dueAt",OffsetDateTime.now().plusDays(2))),t),201).get("id").asText();
        var request=Map.of("scenarioId",scenario("API_URL"));
        perform(as(body(post("/api/assignments/{id}/attempts",a),request),outsider),404);
        perform(as(body(post("/api/assignments/{id}/attempts",a),Map.of("scenarioId",UUID.randomUUID())),s),400);
        perform(as(body(patch("/api/assignments/{id}",a),Map.of("dueAt",OffsetDateTime.now().plusDays(3))),s),403);
        db.jdbc.update("UPDATE assignment SET due_at=? WHERE id=?",OffsetDateTime.now().minusDays(1),UUID.fromString(a));
        perform(as(body(post("/api/assignments/{id}/attempts",a),request),s),409);
        perform(as(body(patch("/api/assignments/{id}",a),Map.of("dueAt",OffsetDateTime.now().plusDays(3))),t),204);
        perform(as(body(patch("/api/groups/{id}",g),Map.of("name","Archived","archived",true)),t),204);
        perform(as(body(post("/api/assignments/{id}/attempts",a),request),s),409);
        perform(as(body(patch("/api/groups/{id}",g),Map.of("name","Reopened","archived",false)),t),204);
        perform(as(body(post("/api/assignments/{id}/attempts",a),request),s),201);
        perform(as(delete("/api/groups/{id}/members/{member}",g,s.id()),t),204);
        perform(as(get("/api/assignments/{id}",a),s),404);
    }
    @Test void jobsNotificationsAndDashboardAreScopedToCurrentUser() throws Exception {
        var s=student();var other=student();
        UUID notification=UUID.randomUUID(),job=UUID.randomUUID();
        db.jdbc.update("INSERT INTO notification(id,user_id,message) VALUES (?,?,?)",notification,s.id(),"Test");
        db.jdbc.update("INSERT INTO background_job(id,owner_id,kind,state,payload,attempts) VALUES (?,?,'PERSONAL_REPORT','FAILED','',3)",job,s.id());
        perform(as(post("/api/jobs/{id}/retry",job),other),404);
        perform(as(post("/api/jobs/{id}/retry",job),s),202);
        perform(as(post("/api/jobs/{id}/retry",job),s),409);
        worker.tick();
        assertThat(perform(as(get("/api/jobs"),s),200).get(0).get("state").asText()).isEqualTo("SUCCEEDED");
        assertThat(perform(as(get("/api/jobs"),other),200).size()).isZero();
        assertThat(perform(as(get("/api/notifications?unreadOnly=true"),s),200).size()).isEqualTo(1);
        perform(as(patch("/api/notifications/read-all"),other),204);
        assertThat(perform(as(get("/api/dashboard"),s),200).get("unreadNotifications").asInt()).isEqualTo(1);
        perform(as(patch("/api/notifications/read-all"),s),204);
        assertThat(perform(as(get("/api/notifications?unreadOnly=true"),s),200).size()).isZero();
        perform(as(get("/api/jobs?size=101"),s),400);
        perform(as(get("/api/assignments?page=-1"),s),400);
        assertThat(perform(as(get("/api/dashboard"),s),200).has("groups")).isFalse();
        assertThat(perform(as(get("/api/dashboard"),teacher()),200).has("groups")).isTrue();
    }
    @Test void assignedWorkIsExplicitAndPersonalPracticeStaysPrivate() throws Exception {
        var t=teacher(); var s=student(); var other=student(); var g=group(t);
        perform(as(body(post("/api/groups/{id}/members",g),Map.of("userId",s.id())),t),204);
        String assignment=perform(as(body(post("/api/groups/{id}/assignments",g),Map.of("workshopId","10000000-0000-0000-0000-000000000001","dueAt",OffsetDateTime.now().plusDays(2))),t),201).get("id").asText();
        UUID personal=start(s,"API_URL"); action(s,personal,"FIX_URL",UUID.randomUUID(),200);
        perform(as(post("/api/attempts/{id}/finish",personal),s),200);
        assertThat(perform(as(get("/api/groups/{id}/statistics",g),t),200).get(0).get("resolved").asInt()).isZero();
        perform(as(get("/api/assignments/{id}",assignment),other),404);
        String assigned=perform(as(body(post("/api/assignments/{id}/attempts",assignment),Map.of("scenarioId",scenario("API_URL"))),s),201).get("id").asText();
        perform(as(get("/api/assignments/{id}/submissions/{attempt}",assignment,assigned),t),404);
        action(s,UUID.fromString(assigned),"FIX_URL",UUID.randomUUID(),200);
        perform(as(post("/api/attempts/{id}/finish",assigned),s),200);
        perform(as(get("/api/assignments/{id}/submissions/{attempt}",assignment,assigned),t),200);
        perform(as(get("/api/assignments/{id}/submissions/{attempt}",assignment,personal),t),404);
        assertThat(perform(as(get("/api/groups/{id}/statistics",g),t),200).get(0).get("resolved").asInt()).isEqualTo(1);
        perform(as(delete("/api/assignments/{id}",assignment),t),204);
        perform(as(body(post("/api/assignments/{id}/attempts",assignment),Map.of("scenarioId",scenario("API_URL"))),s),409);
    }
    @Test void teacherCanManagePublicationAndLocalPdf() throws Exception {
        var t=teacher(); var s=student();
        String w=perform(as(body(post("/api/workshops"),Map.of("title","PDF workshop","description","Practice","topic","Backend","difficulty","BEGINNER","templates",List.of("API_URL"))),t),201).get("id").asText();
        var pdf=new MockMultipartFile("file","guia.pdf","application/pdf","%PDF-1.4\n%%EOF".getBytes());
        String id=perform(as(multipart("/api/workshops/{id}/materials/local",w).file(pdf),t),201).get("id").asText();
        byte[] bytes=mvc.perform(as(get("/api/workshops/{w}/materials/{id}/content",w,id),s)).andReturn().getResponse().getContentAsByteArray();
        assertThat(bytes).isEqualTo(pdf.getBytes());
        perform(as(multipart("/api/workshops/{id}/materials/local",w).file(pdf),s),403);
        perform(as(multipart("/api/workshops/{id}/materials/local",w).file(new MockMultipartFile("file","bad.pdf","application/pdf","bad".getBytes())),t),400);
        perform(as(body(patch("/api/instructor/workshops/{id}",w),Map.of("title","Updated","description","D","topic","Backend","difficulty","BEGINNER","published",false)),t),204);
        perform(as(get("/api/workshops/{id}",w),s),404);
        perform(as(get("/api/instructor/workshops/{id}",w),t),200);
        perform(as(delete("/api/workshops/{w}/materials/{id}",w,id),t),204);
    }
}
