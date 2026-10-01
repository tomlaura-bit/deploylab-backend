package edu.deploylab.dto;
import java.time.Instant;
public record AuthSessionResponse(String token,String refreshToken,Instant expiresAt,UserResponse user) {}
