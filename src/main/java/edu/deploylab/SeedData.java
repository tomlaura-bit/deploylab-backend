package edu.deploylab;

import java.util.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SeedData implements CommandLineRunner {
    private final Db db; private final CatalogService catalog; private final PasswordEncoder passwords;
    @Value("${app.instructor-email}") String email;
    @Value("${app.instructor-password}") String password;
    public SeedData(Db db,CatalogService catalog,PasswordEncoder passwords) {this.db=db;this.catalog=catalog;this.passwords=passwords;}
    @Override @Transactional public void run(String... args) {
        for(var t:CatalogService.TEMPLATES) {
            if(!db.exists("SELECT id FROM skill WHERE name=?",t.skill())) db.jdbc.update("INSERT INTO skill(id,name) VALUES (?,?)",UUID.randomUUID(),t.skill());
        }
        UUID workshop=UUID.fromString("10000000-0000-0000-0000-000000000001");
        if(!db.exists("SELECT id FROM workshop WHERE id=?",workshop)) {
            db.jdbc.update("INSERT INTO workshop(id,title,description,topic,difficulty) VALUES (?,?,?,?,'BEGINNER')",workshop,"Diagnóstico de aplicaciones web","Tres incidentes para practicar APIs, bases de datos y autorización.","Backend");
            CatalogService.TEMPLATES.forEach(t->catalog.addScenario(workshop,t));
        }
        if(!email.isBlank()) {
            if(password.length()<10 || !email.contains("@")) throw new IllegalStateException("Configura credenciales válidas para el instructor inicial");
            AuthService.validatePassword(password);
            String normalized=email.trim().toLowerCase(Locale.ROOT);
            if(!db.exists("SELECT id FROM app_user WHERE email=?",normalized)) {
                db.jdbc.update("INSERT INTO app_user(id,name,email,password_hash,role) VALUES (?,?,?,?,'INSTRUCTOR')",UUID.randomUUID(),"Instructor",normalized,passwords.encode(password));
            }
        }
    }
}
