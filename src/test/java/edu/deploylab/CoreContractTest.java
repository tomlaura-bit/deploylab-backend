package edu.deploylab;

import java.util.*;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

/** Acceptance tests for the seven routes listed in the professor's feedback. */
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class CoreContractTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired Db db;
    JsonNode call(String method,String path,Object body,String token,int status,String key) throws Exception {
        MockHttpServletRequestBuilder r=method.equals("POST")?post(path):get(path);
        if(body!=null) r.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        if(token!=null) r.header("Authorization","Bearer "+token);
        if(key!=null) r.header("Idempotency-Key",key);
        var response=mvc.perform(r).andExpect(status().is(status)).andReturn().getResponse().getContentAsString();
        return response.isBlank()?json.nullNode():json.readTree(response);
    }
    String user() throws Exception {
        String email=UUID.randomUUID()+"@example.test";
        call("POST","/auth/register",Map.of("name","Alumno","email",email,"password","CoreTest123!"),null,201,null);
        return call("POST","/login",Map.of("email",email,"password","CoreTest123!"),null,200,null).get("token").asText();
    }
    String start(String token) throws Exception {
        var list=call("GET","/talleres",null,token,200,null);
        assertThat(list.isArray()).isTrue();
        var workshop=call("GET","/talleres/10000000-0000-0000-0000-000000000001",null,token,200,null);
        JsonNode scenario=null;
        for(var s:workshop.get("scenarios")) if(s.get("title").asText().equals("API mal configurada")) scenario=s;
        assertThat(scenario).isNotNull();
        assertThat(scenario.has("evidence")).isTrue();
        assertThat(scenario.get("actions").get(0).has("correct")).isFalse();
        return call("POST","/escenarios/"+scenario.get("id").asText()+"/intentos",null,token,201,null).get("id").asText();
    }
    JsonNode act(String token,String id,String code,String key,int status) throws Exception {
        return call("POST","/intentos/"+id+"/acciones",Map.of("code",code),token,status,key);
    }
    JsonNode finish(String token,String id,int status) throws Exception {
        return call("POST","/intentos/"+id+"/finalizar",null,token,status,null);
    }
    @Test void sevenRoutesCompleteSuccessWithEvaluationOnlyAtFinalization() throws Exception {
        String token=user(),id=start(token);
        act(token,id,"RESTART",UUID.randomUUID().toString(),200);
        String key=UUID.randomUUID().toString();
        var solved=act(token,id,"FIX_URL",key,200);
        assertThat(solved.get("score").asInt()).isZero();
        var result=finish(token,id,200);
        assertThat(result.get("finalized").asBoolean()).isTrue();
        assertThat(result.get("evaluation").get("score").asInt()).isEqualTo(90);
        assertThat(result.get("events").size()).isEqualTo(2);
        assertThat(finish(token,id,200)).isEqualTo(result);
        assertThat(act(token,id,"FIX_URL",key,200)).isEqualTo(solved);
        act(token,id,"RESTART",UUID.randomUUID().toString(),409);
    }
    @Test void unsuccessfulAttemptCanBeFinalizedWithZeroScoreAndCannotChange() throws Exception {
        String token=user(),id=start(token);
        act(token,id,"RESTART",UUID.randomUUID().toString(),200);
        var result=finish(token,id,200);
        assertThat(result.get("evaluation").get("solved").asBoolean()).isFalse();
        assertThat(result.get("score").asInt()).isZero();
        act(token,id,"FIX_URL",UUID.randomUUID().toString(),409);
    }
    @Test void emptyAttemptAndOtherOwnersAreRejected() throws Exception {
        String token=user(),id=start(token),other=user();
        finish(token,id,409);finish(other,id,404);
        call("POST","/intentos/"+id+"/finalizar",null,null,401,null);
        act(other,id,"FIX_URL",UUID.randomUUID().toString(),404);
        assertThat(db.jdbc.queryForObject("SELECT COUNT(*) FROM attempt_event WHERE attempt_id=?",Integer.class,UUID.fromString(id))).isZero();
    }
    @Test void concurrentFinalizationCreatesOneEvaluation() throws Exception {
        String token=user(),id=start(token);act(token,id,"FIX_URL",UUID.randomUUID().toString(),200);
        try(var pool=Executors.newFixedThreadPool(3)) {
            List<Callable<JsonNode>> calls=List.of(()->finish(token,id,200),()->finish(token,id,200),()->finish(token,id,200));
            var results=pool.invokeAll(calls);
            var first=results.getFirst().get();
            for(var r:results) assertThat(r.get(10,TimeUnit.SECONDS)).isEqualTo(first);
        }
        assertThat(db.jdbc.queryForObject("SELECT COUNT(*) FROM attempt_evaluation WHERE attempt_id=?",Integer.class,UUID.fromString(id))).isEqualTo(1);
    }
}
