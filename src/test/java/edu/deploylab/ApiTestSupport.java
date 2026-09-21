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
abstract class ApiTestSupport {
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
}
