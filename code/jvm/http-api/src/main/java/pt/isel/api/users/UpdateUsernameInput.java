package pt.isel.api.users;

import jakarta.validation.constraints.NotBlank;

public record UpdateUsernameInput(@NotBlank String newUsername, @NotBlank String password) {
}