package edu.deploylab.dto;
import jakarta.validation.constraints.*;
import java.util.List;
public record WorkshopCreateRequest(@NotBlank @Size(max=150) String title,@NotBlank @Size(max=2000) String description,
    @NotBlank @Size(max=60) String topic,@NotNull @Pattern(regexp="BEGINNER|INTERMEDIATE") String difficulty,
    @NotEmpty @Size(max=3) List<@NotBlank String> templates) {}
