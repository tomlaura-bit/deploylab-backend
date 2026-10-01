package edu.deploylab.persistence;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="attempt_event",uniqueConstraints={@UniqueConstraint(name="uk_attempt_request",columnNames={"attempt_id","request_key"}),@UniqueConstraint(name="uk_attempt_event_index",columnNames={"attempt_id","event_index"})})
public class AttemptEventEntity {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="attempt_id",nullable=false) private AttemptEntity attempt;
    @Column(name="request_key",nullable=false) private UUID requestKey;
    @NotBlank @Column(name="action_code",nullable=false,length=60) private String actionCode;
    @NotBlank @Column(name="result_state",nullable=false,length=20) private String resultState;
    @Min(0) @Max(100) @Column(nullable=false) private int score;
    @NotBlank @Size(max=2000) @Column(nullable=false,length=2000) private String feedback;
    @Column(name="event_index",nullable=false) private int eventIndex;
    @Column(name="created_at",nullable=false) private OffsetDateTime createdAt;
    protected AttemptEventEntity() {}
    public UUID getId(){return id;}
}
