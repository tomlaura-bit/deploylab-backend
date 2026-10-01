package edu.deploylab;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import io.jsonwebtoken.JwtException;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    public record Register(@NotBlank @Size(max=100) String name, @NotBlank @Email @Size(max=254) String email,
                           @NotBlank @Size(min=10,max=72) String password) {}
    public record Login(@NotBlank @Email String email, @NotBlank @Size(max=72) String password) {}
    public record Refresh(@NotBlank String refreshToken) {}
    public record Actor(UUID id, String name, String email, String role) {
        public void instructor() { if (!role.equals("INSTRUCTOR")) throw ApiException.forbidden(); }
    }
    public record Session(String token, String refreshToken, Instant expiresAt, Actor user) {}
    private final Db db;
    private final PasswordEncoder passwords;
    private final JwtService jwt;
    private final ApplicationEventPublisher events;
    private final Duration refreshDuration;
    private final SecureRandom random=new SecureRandom();
    private final String dummyHash;
    public AuthService(Db db,PasswordEncoder passwords,JwtService jwt,ApplicationEventPublisher events,@Value("${app.jwt-refresh-days}") long refreshDays) {
        this.db=db; this.passwords=passwords; this.jwt=jwt; this.events=events; this.refreshDuration=Duration.ofDays(refreshDays);
        this.dummyHash=passwords.encode("constant-timing-dummy-password");
    }
    @Transactional
    public Actor register(Register r) {
        validatePassword(r.password());
        UUID id=UUID.randomUUID(); String email=r.email().trim().toLowerCase(Locale.ROOT);
        db.jdbc.update("INSERT INTO app_user(id,name,email,password_hash,role) VALUES (?,?,?,?,'STUDENT')",
            id,r.name().trim(),email,passwords.encode(r.password()));
        var actor=new Actor(id,r.name().trim(),email,"STUDENT");
        events.publishEvent(new UserRegisteredEvent(id,email,Instant.now()));
        return actor;
    }
    @Transactional
    public Session login(Login r) {
        validatePassword(r.password());
        var users=db.jdbc.queryForList("SELECT * FROM app_user WHERE email=?",r.email().trim().toLowerCase(Locale.ROOT));
        String hash=users.isEmpty()?dummyHash:users.getFirst().get("password_hash").toString();
        if (!passwords.matches(r.password(),hash) || users.isEmpty()) throw new InvalidCredentialsException("Credenciales inválidas");
        var u=actor(users.getFirst());
        return issueSession(u);
    }
    public Actor authenticate(String token) {
        try {
            var claims=jwt.verify(token);
            var rows=db.jdbc.queryForList("SELECT u.* FROM app_user u JOIN auth_token t ON t.user_id=u.id WHERE t.token_hash=? AND t.expires_at>CURRENT_TIMESTAMP",digest(token));
            if(rows.isEmpty()) return null;
            var authenticated=actor(rows.getFirst());
            boolean matches=authenticated.id().toString().equals(claims.getSubject())
                && authenticated.email().equals(claims.get("email",String.class))
                && authenticated.role().equals(claims.get("role",String.class));
            return matches?authenticated:null;
        } catch(JwtException|IllegalArgumentException ex) { return null; }
    }
    @Transactional
    public Session refresh(Refresh r) {
        String hash=digest(r.refreshToken());
        var rows=db.jdbc.queryForList("SELECT u.* FROM app_user u JOIN refresh_token t ON t.user_id=u.id WHERE t.token_hash=? AND t.revoked=FALSE AND t.expires_at>CURRENT_TIMESTAMP FOR UPDATE",hash);
        if(rows.isEmpty()) throw new InvalidCredentialsException("Refresh token inválido o vencido");
        var user=actor(rows.getFirst());
        db.jdbc.update("UPDATE refresh_token SET revoked=TRUE WHERE token_hash=?",hash);
        return issueSession(user);
    }
    @Transactional
    public void logout(String token) {
        var user=authenticate(token);
        db.jdbc.update("DELETE FROM auth_token WHERE token_hash=?",digest(token));
        if(user!=null) db.jdbc.update("UPDATE refresh_token SET revoked=TRUE WHERE user_id=?",user.id());
    }
    private Session issueSession(Actor user) {
        var access=jwt.issue(user);
        byte[] bytes=new byte[48]; random.nextBytes(bytes);
        String refresh=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant refreshExpiry=Instant.now().plus(refreshDuration);
        db.jdbc.update("INSERT INTO auth_token(token_hash,user_id,expires_at) VALUES (?,?,?)",digest(access.value()),user.id(),OffsetDateTime.ofInstant(access.expiresAt(),ZoneOffset.UTC));
        db.jdbc.update("INSERT INTO refresh_token(token_hash,user_id,expires_at) VALUES (?,?,?)",digest(refresh),user.id(),OffsetDateTime.ofInstant(refreshExpiry,ZoneOffset.UTC));
        return new Session(access.value(),refresh,access.expiresAt(),user);
    }
    static Actor actor(Map<String,Object> u) { return new Actor(Db.id(u,"id"),u.get("name").toString(),u.get("email").toString(),u.get("role").toString()); }
    static void validatePassword(String password) {
        if(password.getBytes(StandardCharsets.UTF_8).length>72) throw new InvalidRequestException("La contraseña supera 72 bytes");
    }
    static String digest(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
