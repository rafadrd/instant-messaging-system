package pt.isel.api.users;

import jakarta.validation.constraints.NotBlank;

public record UpdatePasswordInput(@NotBlank String oldPassword, @NotBlank String newPassword) {
}