package edu.deploylab.persistence;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="background_job",indexes=@Index(name="idx_job_queue",columnList="state,created_at"))
public class BackgroundJobEntity {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="owner_id",nullable=false) private AppUserEntity owner;
    @NotBlank @Column(nullable=false,length=20) private String kind;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private State state;
    @NotNull @Lob @Column(nullable=false) private String payload;
    @Lob private String result;
    @Min(0) @Column(nullable=false) private int attempts;
    @Size(max=200) @Column(length=200) private String error;
    @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
    @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
    protected BackgroundJobEntity() {}
    public enum State {PENDING,RUNNING,SUCCEEDED,FAILED}
}
