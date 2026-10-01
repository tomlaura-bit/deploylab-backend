package edu.deploylab.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity @Table(name="auth_token",indexes=@Index(name="idx_token_expiry",columnList="expires_at"))
public class AuthTokenEntity {
    @Id @Column(name="token_hash",length=64) private String tokenHash;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private AppUserEntity user;
    @Column(name="expires_at",nullable=false) private OffsetDateTime expiresAt;
    protected AuthTokenEntity() {}
}
