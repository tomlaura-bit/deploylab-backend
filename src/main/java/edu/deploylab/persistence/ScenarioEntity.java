package edu.deploylab.persistence;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.*;

@Entity @Table(name="scenario")
public class ScenarioEntity {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="workshop_id",nullable=false) private WorkshopEntity workshop;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="skill_id",nullable=false) private SkillEntity skill;
    @NotBlank @Size(max=150) @Column(nullable=false,length=150) private String title;
    @NotBlank @Size(max=2000) @Column(nullable=false,length=2000) private String description;
    @NotBlank @Column(name="template_key",nullable=false,length=30) private String templateKey;
    @NotBlank @Size(max=2000) @Column(nullable=false,length=2000) private String evidence;
    @NotBlank @Size(max=1000) @Column(nullable=false,length=1000) private String hint;
    @NotBlank @Size(max=2000) @Column(nullable=false,length=2000) private String explanation;
    @OneToMany(mappedBy="scenario",cascade=CascadeType.ALL,orphanRemoval=true,fetch=FetchType.LAZY) @OrderBy("position ASC") private List<ScenarioStepEntity> steps=new ArrayList<>();
    protected ScenarioEntity() {}
    public UUID getId(){return id;}
}
