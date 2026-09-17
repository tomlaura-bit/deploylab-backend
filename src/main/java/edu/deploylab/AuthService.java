package edu.deploylab;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import jakarta.validation.constraints.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    public record Register(@NotBlank @Size(max=100) String name, @NotBlank @Email @Size(max=254) String email,
                           @NotBlank @Size(min=10,max=72) String password) {}
    public record Login(@NotBlank @Email String email, @NotBlank @Size(max=72) String password) {}
    public record Actor(UUID id, String name, String email, String role) {
        public void instructor() { if (!role.equals("INSTRUCTOR")) throw ApiException.forbidden(); }
    }
    public record Session(String token, Instant expiresAt, Actor user) {}
    private final Db db;
    private final PasswordEncoder passwords;
    private final String dummyHash;
    public AuthService(Db db, PasswordEncoder passwords) {
        this.db=db; this.passwords=passwords; this.dummyHash=passwords.encode("constant-timing-dummy-password");
    }
    @Transactional
    public Actor register(Register r) {
        validatePassword(r.password());
        UUID id=UUID.randomUUID(); String email=r.email().trim().toLowerCase(Locale.ROOT);
        db.jdbc.update("INSERT INTO app_user(id,name,email,password_hash,role) VALUES (?,?,?,?,'STUDENT')",
            id,r.name().trim(),email,passwords.encode(r.password()));
        return new Actor(id,r.name().trim(),email,"STUDENT");
    }
    public Session login(Login r) {
        validatePassword(r.password());
        var users=db.jdbc.queryForList("SELECT * FROM app_user WHERE email=?",r.email().trim().toLowerCase(Locale.ROOT));
        String hash=users.isEmpty()?dummyHash:users.getFirst().get("password_hash").toString();
        if (!passwords.matches(r.password(),hash) || users.isEmpty()) throw new ApiException(401,"Credenciales inválidas");
        var u=actor(users.getFirst());
        byte[] bytes=new byte[32]; new SecureRandom().nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiry=Instant.now().plus(Duration.ofHours(8));
        db.jdbc.update("INSERT INTO auth_token(token_hash,user_id,expires_at) VALUES (?,?,?)",digest(token),u.id(),OffsetDateTime.ofInstant(expiry,ZoneOffset.UTC));
        return new Session(token,expiry,u);
    }
    public Actor authenticate(String token) {
        var rows=db.jdbc.queryForList("SELECT u.* FROM app_user u JOIN auth_token t ON t.user_id=u.id WHERE t.token_hash=? AND t.expires_at>CURRENT_TIMESTAMP",digest(token));
        return rows.isEmpty()?null:actor(rows.getFirst());
    }
    public void logout(String token) { db.jdbc.update("DELETE FROM auth_token WHERE token_hash=?",digest(token)); }
    static Actor actor(Map<String,Object> u) { return new Actor(Db.id(u,"id"),u.get("name").toString(),u.get("email").toString(),u.get("role").toString()); }
    static void validatePassword(String password) {
        if(password.getBytes(StandardCharsets.UTF_8).length>72) throw new ApiException(400,"La contraseña supera 72 bytes");
    }
    static String digest(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
