package pt.isel.api.channels;

import jakarta.validation.constraints.NotBlank;

public record JoinByTokenInput(@NotBlank String token) {
}