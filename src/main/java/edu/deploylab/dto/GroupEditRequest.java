package edu.deploylab.dto;
import jakarta.validation.constraints.*;
public record GroupEditRequest(@NotBlank @Size(max=120) String name,@NotNull Boolean archived) {}
