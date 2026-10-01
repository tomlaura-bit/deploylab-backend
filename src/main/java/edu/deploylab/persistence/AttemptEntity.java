package edu.deploylab.persistence;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.*;

@Entity @Table(name="attempt",indexes={@Index(name="idx_attempt_user",columnList="user_id,created_at"),@Index(name="attempt_assignment_idx",columnList="assignment_id,user_id")})
public class AttemptEntity {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private AppUserEntity user;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="scenario_id",nullable=false) private ScenarioEntity scenario;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="assignment_id") private AssignmentEntity assignment;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private State state;
    @Min(0) @Column(nullable=false) private int mistakes;
    @Min(0) @Column(nullable=false) private int hints;
    @Min(0) @Max(100) @Column(nullable=false) private int score;
    @Column(nullable=false) private boolean finalized;
    @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
    @Column(name="completed_at") private OffsetDateTime completedAt;
    @OneToMany(mappedBy="attempt",cascade=CascadeType.ALL,orphanRemoval=true,fetch=FetchType.LAZY) @OrderBy("eventIndex ASC") private List<AttemptEventEntity> events=new ArrayList<>();
    @OneToOne(mappedBy="attempt",cascade=CascadeType.ALL,orphanRemoval=true,fetch=FetchType.LAZY) private AttemptEvaluationEntity evaluation;
    protected AttemptEntity() {}
    public UUID getId(){return id;} public enum State {IN_PROGRESS,RESOLVED}
}
