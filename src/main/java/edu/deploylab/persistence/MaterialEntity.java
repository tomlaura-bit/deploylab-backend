package edu.deploylab.persistence;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.UUID;

@Entity @Table(name="material")
public class MaterialEntity {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="workshop_id",nullable=false) private WorkshopEntity workshop;
    @NotBlank @Size(max=120) @Column(nullable=false,length=120) private String filename;
    @NotBlank @Size(max=300) @Column(name="object_key",nullable=false,unique=true,length=300) private String objectKey;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Status status;
    @Lob @Basic(fetch=FetchType.LAZY) private byte[] content;
    @Column(nullable=false) private boolean deleted;
    protected MaterialEntity() {}
    public enum Status {PENDING,READY}
}
