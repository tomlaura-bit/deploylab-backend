package edu.deploylab.persistence;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.*;

@Entity @Table(name="scenario_step",uniqueConstraints=@UniqueConstraint(name="uk_scenario_step_position",columnNames={"scenario_id","position"}))
public class ScenarioStepEntity {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="scenario_id",nullable=false) private ScenarioEntity scenario;
    @Min(1) @Column(nullable=false) private int position;
    @NotBlank @Size(max=150) @Column(nullable=false,length=150) private String title;
    @OneToMany(mappedBy="step",cascade=CascadeType.ALL,orphanRemoval=true,fetch=FetchType.LAZY) private List<ScenarioActionEntity> actions=new ArrayList<>();
    protected ScenarioStepEntity() {}
    public UUID getId(){return id;}
}
