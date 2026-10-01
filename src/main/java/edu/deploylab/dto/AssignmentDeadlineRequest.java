package edu.deploylab.dto;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
public record AssignmentDeadlineRequest(@NotNull @Future OffsetDateTime dueAt) {}
