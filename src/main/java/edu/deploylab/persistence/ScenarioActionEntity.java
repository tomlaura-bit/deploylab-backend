package edu.deploylab.persistence;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.UUID;

@Entity @Table(name="scenario_action",uniqueConstraints=@UniqueConstraint(name="uk_step_action_code",columnNames={"step_id","code"}))
public class ScenarioActionEntity {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="step_id",nullable=false) private ScenarioStepEntity step;
    @NotBlank @Size(max=60) @Column(nullable=false,length=60) private String code;
    @NotBlank @Size(max=150) @Column(nullable=false,length=150) private String label;
    @Column(nullable=false) private boolean correct;
    protected ScenarioActionEntity() {}
    public UUID getId(){return id;}
}
