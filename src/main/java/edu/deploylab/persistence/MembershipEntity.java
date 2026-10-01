package edu.deploylab.persistence;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity @Table(name="membership")
public class MembershipEntity {
    @EmbeddedId private MembershipId id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @MapsId("groupId") @JoinColumn(name="group_id",nullable=false) private StudyGroupEntity group;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @MapsId("userId") @JoinColumn(name="user_id",nullable=false) private AppUserEntity user;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private AppUserEntity.Role role;
    protected MembershipEntity() {}
}
