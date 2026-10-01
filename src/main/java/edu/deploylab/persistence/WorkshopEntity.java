package edu.deploylab.persistence;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.*;

@Entity @Table(name="workshop")
public class WorkshopEntity {
    @Id private UUID id;
    @NotBlank @Size(max=150) @Column(nullable=false,length=150) private String title;
    @NotBlank @Size(max=2000) @Column(nullable=false,length=2000) private String description;
    @NotBlank @Size(max=60) @Column(nullable=false,length=60) private String topic;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Difficulty difficulty;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="owner_id") private AppUserEntity owner;
    @Column(nullable=false) private boolean published;
    @OneToMany(mappedBy="workshop",cascade=CascadeType.ALL,orphanRemoval=true,fetch=FetchType.LAZY) private List<ScenarioEntity> scenarios=new ArrayList<>();
    @ManyToMany(fetch=FetchType.LAZY) @JoinTable(name="workshop_skill",joinColumns=@JoinColumn(name="workshop_id"),inverseJoinColumns=@JoinColumn(name="skill_id")) private Set<SkillEntity> skills=new LinkedHashSet<>();
    @OneToMany(mappedBy="workshop",fetch=FetchType.LAZY) private List<MaterialEntity> materials=new ArrayList<>();
    protected WorkshopEntity() {}
    public UUID getId(){return id;} public enum Difficulty {BEGINNER,INTERMEDIATE}
}
