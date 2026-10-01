package edu.deploylab.persistence;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.*;

@Entity @Table(name="skill")
public class SkillEntity {
    @Id private UUID id;
    @NotBlank @Size(max=80) @Column(nullable=false,unique=true,length=80) private String name;
    @ManyToMany(mappedBy="skills",fetch=FetchType.LAZY) private Set<WorkshopEntity> workshops=new LinkedHashSet<>();
    @OneToMany(mappedBy="skill",fetch=FetchType.LAZY) private List<ScenarioEntity> scenarios=new ArrayList<>();
    protected SkillEntity() {}
    public UUID getId(){return id;}
}
