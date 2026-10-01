package edu.deploylab.dto;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.UUID;
public record AssignmentCreateRequest(@NotNull UUID workshopId,@NotNull @Future OffsetDateTime dueAt) {}
