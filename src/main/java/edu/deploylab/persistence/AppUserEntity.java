package edu.deploylab.persistence;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.*;

@Entity @Table(name="app_user", indexes=@Index(name="idx_user_email",columnList="email",unique=true))
public class AppUserEntity {
    @Id private UUID id;
    @NotBlank @Size(max=100) @Column(nullable=false,length=100) private String name;
    @NotBlank @Email @Size(max=254) @Column(nullable=false,unique=true,length=254) private String email;
    @NotBlank @Column(name="password_hash",nullable=false,length=100) private String passwordHash;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Role role;
    @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
    @OneToMany(mappedBy="user",fetch=FetchType.LAZY) private List<AttemptEntity> attempts=new ArrayList<>();
    @OneToMany(mappedBy="owner",fetch=FetchType.LAZY) private List<WorkshopEntity> workshops=new ArrayList<>();
    @OneToMany(mappedBy="instructor",fetch=FetchType.LAZY) private List<StudyGroupEntity> instructedGroups=new ArrayList<>();
    @OneToMany(mappedBy="user",fetch=FetchType.LAZY) private List<NotificationEntity> notifications=new ArrayList<>();
    protected AppUserEntity() {}
    public UUID getId(){return id;} public String getEmail(){return email;} public Role getRole(){return role;}
    public enum Role {STUDENT,INSTRUCTOR}
}
