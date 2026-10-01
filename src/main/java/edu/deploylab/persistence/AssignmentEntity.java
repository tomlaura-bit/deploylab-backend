package edu.deploylab.persistence;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.*;

@Entity @Table(name="assignment",uniqueConstraints=@UniqueConstraint(name="uk_group_workshop",columnNames={"group_id","workshop_id"}))
public class AssignmentEntity {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="group_id",nullable=false) private StudyGroupEntity group;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="workshop_id",nullable=false) private WorkshopEntity workshop;
    @NotNull @Column(name="due_at",nullable=false) private OffsetDateTime dueAt;
    @Column(nullable=false) private boolean cancelled;
    @OneToMany(mappedBy="assignment",fetch=FetchType.LAZY) private List<AttemptEntity> attempts=new ArrayList<>();
    protected AssignmentEntity() {}
    public UUID getId(){return id;}
}
