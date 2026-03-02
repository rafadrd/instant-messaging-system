package pt.isel.api.messages;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageRequest(@NotBlank @Size(min = 1, max = 1000) String content) {
}