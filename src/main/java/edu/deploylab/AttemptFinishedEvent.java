package edu.deploylab;
import java.time.Instant;
import java.util.UUID;
public record AttemptFinishedEvent(UUID attemptId,UUID userId,int score,boolean solved,Instant occurredAt) {}
