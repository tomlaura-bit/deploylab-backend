package edu.deploylab;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.extras-enabled",havingValue="true")
    org.springdoc.core.models.GroupedOpenApi modulesApi() {
        return org.springdoc.core.models.GroupedOpenApi.builder().group("modules").pathsToMatch("/api/**").build();
    }
    @Bean org.springdoc.core.models.GroupedOpenApi coreApi() {
        return org.springdoc.core.models.GroupedOpenApi.builder().group("core")
            .pathsToMatch("/auth/register","/login","/talleres","/talleres/*","/escenarios/*/intentos","/intentos/*/acciones","/intentos/*/finalizar").build();
    }
    @Bean OpenAPI api() {
        return new OpenAPI().info(new Info().title("DeployLab API").version("1.0.0")
            .description("Inicia sesión en /api/auth/login y pega el token en Authorize. Los tokens opacos vencen en 8 horas."))
            .components(new Components().addSecuritySchemes("bearer",new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")))
            .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
