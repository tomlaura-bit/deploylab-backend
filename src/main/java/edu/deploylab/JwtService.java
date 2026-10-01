package edu.deploylab;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key; private final Duration accessDuration;
    public JwtService(@Value("${app.jwt-secret}") String secret,@Value("${app.jwt-access-minutes}") long accessMinutes) {
        if(secret.getBytes(StandardCharsets.UTF_8).length<32) throw new IllegalStateException("JWT_SECRET debe contener al menos 32 bytes");
        key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); accessDuration=Duration.ofMinutes(accessMinutes);
    }
    public AccessToken issue(AuthService.Actor actor) {
        Instant issued=Instant.now(),expires=issued.plus(accessDuration);
        String token=Jwts.builder().issuer("deploylab").subject(actor.id().toString()).id(UUID.randomUUID().toString())
            .claim("email",actor.email()).claim("role",actor.role()).issuedAt(Date.from(issued)).expiration(Date.from(expires))
            .signWith(key).compact();
        return new AccessToken(token,expires);
    }
    public Claims verify(String token) {
        return Jwts.parser().verifyWith(key).requireIssuer("deploylab").build().parseSignedClaims(token).getPayload();
    }
    public record AccessToken(String value,Instant expiresAt) {}
}
