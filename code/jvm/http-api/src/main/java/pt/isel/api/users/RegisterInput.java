package pt.isel.api.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterInput(
        @NotBlank @Size(min = 1, max = 30) String username,
        @NotBlank String password,
        String invitationToken
) {
}