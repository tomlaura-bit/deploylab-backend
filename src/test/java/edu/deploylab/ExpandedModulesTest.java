package edu.deploylab;

import java.util.*;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.assertj.core.api.Assertions.*;

class ExpandedModulesTest extends ApiIntegrationTest {
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
