package edu.deploylab.persistence;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="attempt_evaluation")
public class AttemptEvaluationEntity {
    @Id @Column(name="attempt_id") private UUID attemptId;
    @OneToOne(fetch=FetchType.LAZY,optional=false) @MapsId @JoinColumn(name="attempt_id") private AttemptEntity attempt;
    @Min(0) @Max(100) @Column(nullable=false) private int score;
    @Min(0) @Column(nullable=false) private int mistakes;
    @Min(0) @Column(nullable=false) private int hints;
    @Column(nullable=false) private boolean solved;
    @NotBlank @Size(max=2000) @Column(nullable=false,length=2000) private String explanation;
    @Column(name="evaluated_at",nullable=false) private OffsetDateTime evaluatedAt;
    protected AttemptEvaluationEntity() {}
}
