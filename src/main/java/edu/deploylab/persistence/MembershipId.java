package edu.deploylab.persistence;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.*;

@Embeddable
public class MembershipId implements Serializable {
    private UUID groupId;
    private UUID userId;
    protected MembershipId() {}
    @Override public boolean equals(Object value){return value instanceof MembershipId other&&Objects.equals(groupId,other.groupId)&&Objects.equals(userId,other.userId);}
    @Override public int hashCode(){return Objects.hash(groupId,userId);}
}
