package edu.deploylab.persistence;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="notification",indexes=@Index(name="notification_user_idx",columnList="user_id,created_at"))
public class NotificationEntity {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private AppUserEntity user;
    @NotBlank @Size(max=1000) @Column(nullable=false,length=1000) private String message;
    @Column(name="read_at") private OffsetDateTime readAt;
    @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
    protected NotificationEntity() {}
}
