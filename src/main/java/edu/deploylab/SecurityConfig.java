package edu.deploylab;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean SecurityFilterChain security(HttpSecurity http,JwtAuthenticationFilter jwtFilter,ObjectMapper mapper,@Value("${app.cors-origin}") String origin) throws Exception {
        CorsConfiguration cors=new CorsConfiguration(); cors.setAllowedOrigins(List.of(origin));
        cors.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization","Content-Type","Idempotency-Key"));
        UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**",cors);
        http.csrf(c->c.disable()).cors(c->c.configurationSource(source))
            .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a.requestMatchers(HttpMethod.POST,"/api/auth/register","/api/auth/login","/api/auth/refresh","/auth/register","/auth/login","/auth/refresh","/login").permitAll()
                .requestMatchers("/actuator/health","/swagger-ui/**","/swagger-ui.html","/v3/api-docs/**").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->json(mapper,req,res,401,"Unauthorized","Autenticación requerida"))
                .accessDeniedHandler((req,res,ex)->json(mapper,req,res,403,"Forbidden","Acceso denegado")))
            .addFilterBefore(jwtFilter,UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    static void json(ObjectMapper mapper,HttpServletRequest req,HttpServletResponse res,int status,String error,String message) throws IOException {
        res.setStatus(status); res.setContentType("application/json;charset=UTF-8");
        mapper.writeValue(res.getWriter(),ErrorResponse.of(status,error,message,req.getRequestURI()));
    }
}
