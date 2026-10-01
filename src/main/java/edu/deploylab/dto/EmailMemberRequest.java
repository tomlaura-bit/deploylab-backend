package edu.deploylab.dto;
import jakarta.validation.constraints.*;
public record EmailMemberRequest(@NotBlank @Email @Size(max=254) String email) {}
