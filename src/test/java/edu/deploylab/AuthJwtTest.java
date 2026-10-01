package edu.deploylab;

import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class AuthJwtTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    JsonNode postJson(String path,Object body,int expected) throws Exception {
        var response=mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)))
            .andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return response.isBlank()?json.nullNode():json.readTree(response);
    }

    @Test void loginIssuesJwtAndRefreshTokenCanOnlyRotateOnce() throws Exception {
        String email=UUID.randomUUID()+"@jwt.test";
        postJson("/auth/register",Map.of("name","JWT User","email",email,"password","JwtPassword123!"),201);
        var first=postJson("/auth/login",Map.of("email",email,"password","JwtPassword123!"),200);
        assertThat(first.get("token").asText().split("\\.")).hasSize(3);
        assertThat(first.get("refreshToken").asText()).isNotBlank();

        var second=postJson("/auth/refresh",Map.of("refreshToken",first.get("refreshToken").asText()),200);
        assertThat(second.get("token").asText()).isNotEqualTo(first.get("token").asText());
        assertThat(second.get("refreshToken").asText()).isNotEqualTo(first.get("refreshToken").asText());
        postJson("/auth/refresh",Map.of("refreshToken",first.get("refreshToken").asText()),401);
    }

    @Test void logoutRevokesAccessAndRefreshTokens() throws Exception {
        String email=UUID.randomUUID()+"@jwt.test";
        postJson("/auth/register",Map.of("name","Logout User","email",email,"password","JwtPassword123!"),201);
        var session=postJson("/auth/login",Map.of("email",email,"password","JwtPassword123!"),200);
        String access=session.get("token").asText();
        mvc.perform(post("/auth/logout").header("Authorization","Bearer "+access)).andExpect(status().isNoContent());
        mvc.perform(get("/auth/me").header("Authorization","Bearer "+access)).andExpect(status().isUnauthorized());
        postJson("/auth/refresh",Map.of("refreshToken",session.get("refreshToken").asText()),401);
    }

    @Test void errorsUseOneDocumentedShape() throws Exception {
        var response=postJson("/auth/login",Map.of("email","invalid","password","short"),400);
        assertThat(response.fieldNames()).toIterable().containsExactlyInAnyOrder(
            "timestamp","status","error","message","path");
        assertThat(response.get("path").asText()).isEqualTo("/auth/login");
    }
}
