package edu.deploylab;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired Db db;
    @Autowired JobWorker worker;
    @Autowired AuthService auth;
    @Autowired AttemptService attempts;
    record User(String token,UUID id) {}
    JsonNode perform(MockHttpServletRequestBuilder req,int status) throws Exception {
        String response=mvc.perform(req).andExpect(status().is(status)).andReturn().getResponse().getContentAsString();
        return response.isBlank()?json.nullNode():json.readTree(response);
    }
    MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder req,Object body) throws Exception {
        return req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    }
    MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder req,User user) {return req.header("Authorization","Bearer "+user.token());}
    User student() throws Exception {
        String email=UUID.randomUUID()+"@example.test";
        var u=perform(body(post("/api/auth/register"),Map.of("name","Student","email",email,"password","SecureTest123!")),201);
        var login=perform(body(post("/api/auth/login"),Map.of("email",email,"password","SecureTest123!")),200);
        return new User(login.get("token").asText(),UUID.fromString(u.get("id").asText()));
    }
    User teacher() throws Exception {
        var l=perform(body(post("/api/auth/login"),Map.of("email","teacher@deploylab.test","password","TestingOnly123!")),200);
        return new User(l.get("token").asText(),UUID.fromString(l.get("user").get("id").asText()));
    }
    UUID scenario(String key) {return Db.id(db.one("SELECT id FROM scenario WHERE template_key=? AND workshop_id=?",key,UUID.fromString("10000000-0000-0000-0000-000000000001")),"id");}
    UUID start(User u,String template) throws Exception {
        return UUID.fromString(perform(as(body(post("/api/attempts"),Map.of("scenarioId",scenario(template))),u),201).get("id").asText());
    }
    JsonNode action(User u,UUID id,String code,UUID key,int status) throws Exception {
        return perform(as(body(post("/api/attempts/{id}/actions",id),Map.of("code",code)),u).header("Idempotency-Key",key),status);
    }
    UUID group(User teacher) throws Exception {
        return UUID.fromString(perform(as(body(post("/api/groups"),Map.of("name","Grupo de prueba")),teacher),201).get("id").asText());
    }
    @Test void publicHealthAndDocumentationAreAvailable() throws Exception {
        perform(get("/actuator/health"),200);perform(get("/v3/api-docs"),200);
    }
    @Test void missingOrForgedTokenIsUnauthorized() throws Exception {
        perform(get("/api/workshops"),401);perform(get("/api/workshops").header("Authorization","Bearer invalid"),401);
    }
    @Test void registerLoginAndLogoutInvalidateToken() throws Exception {
        User u=student(); var me=perform(as(get("/api/auth/me"),u),200);
        assertThat(me.get("role").asText()).isEqualTo("STUDENT");
        assertThat(me.has("password_hash")).isFalse();
        perform(as(post("/api/auth/logout"),u),204); perform(as(get("/api/auth/me"),u),401);
    }
    @Test void expiredTokenIsRejected() throws Exception {
        User u=student(); db.jdbc.update("UPDATE auth_token SET expires_at=? WHERE user_id=?",OffsetDateTime.now().minusHours(1),u.id());
        perform(as(get("/api/auth/me"),u),401);
    }
    @Test void duplicateEmailInvalidPasswordAndRoleEscalationAreRejected() throws Exception {
        String email=UUID.randomUUID()+"@test.local";
        Map<String,String> r=Map.of("name","Student","email",email,"password","SecureTest123!");
        perform(body(post("/api/auth/register"),r),201);perform(body(post("/api/auth/register"),r),409);
        perform(body(post("/api/auth/login"),Map.of("email",email,"password","WrongPass123!")),401);
        perform(body(post("/api/auth/register"),Map.of("name","Test","email","invalid","password","short")),400);
        perform(body(post("/api/auth/register"),Map.of("name","Test","email","a@b.test","password","SecureTest123!","role","INSTRUCTOR")),400);
    }
    @Test void catalogFiltersAndHidesSolutions() throws Exception {
        User u=student(); var list=perform(as(get("/api/workshops?topic=Backend&difficulty=BEGINNER&size=2"),u),200);
        assertThat(list.isArray()).isTrue();assertThat(list.size()).isBetween(1,2);
        perform(as(get("/api/workshops?size=1000"),u),400);
        var s=perform(as(get("/api/scenarios/{id}",scenario("API_URL")),u),200);
        assertThat(s.has("explanation")).isFalse();assertThat(s.get("actions").get(0).has("correct")).isFalse();
        perform(as(get("/api/templates"),u),403);
    }
    @Test void allThreeScenariosCanBeResolved() throws Exception {
        User u=student();
        for(var t:CatalogService.TEMPLATES) {
            UUID id=start(u,t.key());
            var result=action(u,id,t.correctAction(),UUID.randomUUID(),200);
            assertThat(result.get("result_state").asText()).isEqualTo("RESOLVED");
            assertThat(result.get("score").asInt()).isZero();
            var evaluation=perform(as(post("/api/attempts/{id}/finish",id),u),200);
            assertThat(evaluation.get("score").asInt()).isEqualTo(100);
            var detail=perform(as(get("/api/attempts/{id}",id),u),200);
            assertThat(detail.get("explanation").asText()).isNotBlank();
        }
        assertThat(perform(as(get("/api/attempts"),u),200).size()).isEqualTo(3);
        assertThat(perform(as(get("/api/progress"),u),200).size()).isGreaterThanOrEqualTo(3);
    }
    @Test void wrongActionAndHintReduceScoreAndRepeatHintIsFree() throws Exception {
        User u=student();UUID id=start(u,"API_URL");
        action(u,id,"RESTART",UUID.randomUUID(),200);
        perform(as(post("/api/attempts/{id}/hint",id),u),200);perform(as(post("/api/attempts/{id}/hint",id),u),200);
        var result=action(u,id,"FIX_URL",UUID.randomUUID(),200);
        assertThat(result.get("score").asInt()).isZero();
        assertThat(perform(as(post("/api/attempts/{id}/finish",id),u),200).get("score").asInt()).isEqualTo(85);
        assertThat(db.one("SELECT hints,mistakes FROM attempt WHERE id=?",id)).containsEntry("hints",1).containsEntry("mistakes",1);
    }
    @Test void solvingApiScenarioDoesNotAwardDatabaseOrAuthorizationProgress() throws Exception {
        User u=student();UUID id=start(u,"API_URL");action(u,id,"FIX_URL",UUID.randomUUID(),200);
        perform(as(post("/api/attempts/{id}/finish",id),u),200);
        var progress=perform(as(get("/api/progress"),u),200);
        for(var skill:progress) {
            int expected=skill.get("name").asText().equals("Diagnóstico de APIs")?1:0;
            assertThat(skill.get("resolved_attempts").asInt()).isEqualTo(expected);
        }
    }
    @Test void invalidActionDoesNotMutateAttemptAndNeedsIdempotencyKey() throws Exception {
        User u=student();UUID id=start(u,"API_URL");
        action(u,id,"FIX_ROLE",UUID.randomUUID(),400);
        perform(as(body(post("/api/attempts/{id}/actions",id),Map.of("code","FIX_URL")),u),400);
        assertThat(db.one("SELECT mistakes FROM attempt WHERE id=?",id).get("mistakes")).isEqualTo(0);
        assertThat(db.jdbc.queryForObject("SELECT COUNT(*) FROM attempt_event WHERE attempt_id=?",Integer.class,id)).isZero();
    }
    @Test void idempotencyReplaysOriginalResultAndRejectsKeyReuse() throws Exception {
        User u=student();UUID id=start(u,"API_URL"),key=UUID.randomUUID();
        var first=action(u,id,"FIX_URL",key,200);var second=action(u,id,"FIX_URL",key,200);
        assertThat(second).isEqualTo(first);
        action(u,id,"RESTART",key,409);action(u,id,"FIX_URL",UUID.randomUUID(),409);
        perform(as(post("/api/attempts/{id}/hint",id),u),409);
        assertThat(db.jdbc.queryForObject("SELECT COUNT(*) FROM attempt_event WHERE attempt_id=?",Integer.class,id)).isEqualTo(1);
    }
    @Test void anotherStudentCannotReadOrChangeAttempt() throws Exception {
        User owner=student(),other=student();UUID id=start(owner,"API_URL");
        perform(as(get("/api/attempts/{id}",id),other),404);
        action(other,id,"FIX_URL",UUID.randomUUID(),404);perform(as(post("/api/attempts/{id}/hint",id),other),404);
    }
    @Test void concurrentIdenticalRequestsProduceOneEvent() throws Exception {
        User u=student(); UUID id=start(u,"API_URL"),key=UUID.randomUUID();
        var actor=auth.authenticate(u.token());
        try(var pool=Executors.newFixedThreadPool(4)) {
            List<Callable<Map<String,Object>>> calls=new ArrayList<>();
            for(int n=0;n<4;n++) calls.add(()->attempts.action(id,actor,key,"FIX_URL"));
            var results=pool.invokeAll(calls);
            Set<Object> eventIds=new HashSet<>();for(var result:results) eventIds.add(result.get(10,TimeUnit.SECONDS).get("id"));
            assertThat(eventIds).hasSize(1);
        }
    }
    @Test void studentsCannotCreateWorkshopsOrGroups() throws Exception {
        User u=student();
        perform(as(body(post("/api/groups"),Map.of("name","Forbidden")),u),403);
        perform(as(body(post("/api/workshops"),Map.of("title","T","description","D","topic","Backend","difficulty","BEGINNER","templates",List.of("API_URL"))),u),403);
    }
    @Test void instructorCreatesTemplateWorkshopAndRejectsUnknownTemplate() throws Exception {
        User t=teacher();
        var created=perform(as(body(post("/api/workshops"),Map.of("title","Mi taller","description","D","topic","Backend","difficulty","BEGINNER","templates",List.of("API_URL","DB_AUTH"))),t),201);
        var workshop=perform(as(get("/api/workshops/{id}",created.get("id").asText()),t),200);
        assertThat(workshop.get("scenarios").size()).isEqualTo(2);
        perform(as(body(post("/api/workshops"),Map.of("title","T","description","D","topic","B","difficulty","BEGINNER","templates",List.of("UNKNOWN"))),t),400);
    }
    @Test void groupAssignmentNotifiesMembersAndEnforcesOwner() throws Exception {
        User t=teacher(),s=student(),outsider=student(); UUID g=group(t);
        perform(as(body(post("/api/groups/{id}/members",g),Map.of("userId",s.id())),t),204);
        perform(as(body(post("/api/groups/{id}/members",g),Map.of("userId",s.id())),t),409);
        perform(as(get("/api/groups/{id}",g),outsider),404);
        UUID workshop=UUID.fromString("10000000-0000-0000-0000-000000000001");
        var assignment=Map.of("workshopId",workshop,"dueAt",OffsetDateTime.now().plusDays(7).toString());
        perform(as(body(post("/api/groups/{id}/assignments",g),assignment),t),201);
        perform(as(body(post("/api/groups/{id}/assignments",g),assignment),t),409);
        var notifications=perform(as(get("/api/notifications"),s),200);
        assertThat(notifications.size()).isEqualTo(1);
        String notification=notifications.get(0).get("id").asText();
        perform(as(patch("/api/notifications/{id}/read",notification),outsider),404);
        perform(as(patch("/api/notifications/{id}/read",notification),s),204);
        UUID a=start(s,"API_URL");action(s,a,"FIX_URL",UUID.randomUUID(),200);
        perform(as(post("/api/attempts/{id}/finish",a),s),200);
        var stats=perform(as(get("/api/groups/{id}/statistics",g),t),200);
        assertThat(stats.get(0).get("resolved").asInt()).isEqualTo(1);
        perform(as(get("/api/groups/{id}/statistics",g),s),403);
        var report=perform(as(post("/api/groups/{id}/reports",g),t),202);worker.tick();
        mvc.perform(as(get("/api/jobs/{id}/download",report.get("id").asText()),t)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("RESOLVED")));
        perform(as(delete("/api/groups/{id}/members/{member}",g,s.id()),t),204);
        perform(as(get("/api/groups/{id}",g),s),404);
    }
    @Test void reportIsAsynchronousAndPrivate() throws Exception {
        User s=student(),other=student();UUID a=start(s,"DB_AUTH");action(s,a,"FIX_CREDENTIALS",UUID.randomUUID(),200);
        perform(as(post("/api/attempts/{id}/finish",a),s),200);
        var report=perform(as(post("/api/reports"),s),202);String id=report.get("id").asText();
        perform(as(get("/api/jobs/{id}/download",id),s),409);
        perform(as(get("/api/jobs/{id}",id),other),404);worker.tick();
        var state=perform(as(get("/api/jobs/{id}",id),s),200);
        assertThat(state.get("state").asText()).isEqualTo("SUCCEEDED");
        mvc.perform(as(get("/api/jobs/{id}/download",id),s)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("RESOLVED")));
    }
    @Test void unavailableEmailRetriesAndEndsFailedWithoutBlockingAssignment() throws Exception {
        User s=student();UUID job=UUID.randomUUID();
        db.jdbc.update("INSERT INTO background_job(id,owner_id,kind,state,payload) VALUES (?,?,'EMAIL','PENDING','Test')",job,s.id());
        worker.tick();worker.tick();worker.tick();
        assertThat(db.one("SELECT state,attempts FROM background_job WHERE id=?",job)).containsEntry("state","FAILED").containsEntry("attempts",3);
    }
    @Test void unconfiguredS3ReturnsExplicitServiceUnavailable() throws Exception {
        User t=teacher();
        String workshop=perform(as(body(post("/api/workshops"),Map.of("title","Materiales","description","D","topic","B","difficulty","BEGINNER","templates",List.of("API_URL"))),t),201).get("id").asText();
        perform(as(body(post("/api/workshops/{id}/materials",workshop),Map.of("filename","guia.pdf")),t),503);
        perform(as(body(post("/api/workshops/{id}/materials",workshop),Map.of("filename","../escape.pdf")),t),400);
    }
}
