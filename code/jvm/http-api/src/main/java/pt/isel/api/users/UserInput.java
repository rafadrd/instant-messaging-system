package pt.isel.api.users;

import jakarta.validation.constraints.NotBlank;

public record UserInput(@NotBlank String username, @NotBlank String password) {
}