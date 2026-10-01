package edu.deploylab.dto;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record AttemptStartRequest(@NotNull UUID scenarioId) {}
