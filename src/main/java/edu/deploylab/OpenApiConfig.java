package edu.deploylab;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {
    @Bean OpenAPI api() {
        return new OpenAPI().info(new Info().title("DeployLab API").version("1.0.0")
            .description("Inicia sesión en /api/auth/login y pega el token en Authorize. Los tokens opacos vencen en 8 horas."))
            .components(new Components().addSecuritySchemes("bearer",new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")))
            .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
