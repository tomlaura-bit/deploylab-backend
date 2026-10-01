package edu.deploylab.dto;
import jakarta.validation.constraints.*;
public record GroupCreateRequest(@NotBlank @Size(max=120) String name) {}
