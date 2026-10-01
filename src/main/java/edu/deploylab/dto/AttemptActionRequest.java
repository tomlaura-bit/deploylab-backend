package edu.deploylab.dto;
import jakarta.validation.constraints.*;
public record AttemptActionRequest(@NotBlank @Size(max=60) String code) {}
