package edu.deploylab;
import java.time.Instant;
import java.util.UUID;
public record AssignmentCreatedEvent(UUID assignmentId,UUID groupId,UUID workshopId,Instant occurredAt) {}
