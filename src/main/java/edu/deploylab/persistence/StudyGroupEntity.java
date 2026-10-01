package edu.deploylab.persistence;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.*;

@Entity @Table(name="study_group")
public class StudyGroupEntity {
    @Id private UUID id;
    @NotBlank @Size(max=120) @Column(nullable=false,length=120) private String name;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="instructor_id",nullable=false) private AppUserEntity instructor;
    @Column(nullable=false) private boolean archived;
    @OneToMany(mappedBy="group",cascade=CascadeType.ALL,orphanRemoval=true,fetch=FetchType.LAZY) private List<MembershipEntity> memberships=new ArrayList<>();
    @OneToMany(mappedBy="group",cascade=CascadeType.ALL,orphanRemoval=true,fetch=FetchType.LAZY) private List<AssignmentEntity> assignments=new ArrayList<>();
    protected StudyGroupEntity() {}
    public UUID getId(){return id;}
}
