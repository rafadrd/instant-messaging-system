package pt.isel.domain.users;

import pt.isel.domain.security.PasswordValidationInfo;

public record User(
        Long id,
        String username,
        PasswordValidationInfo passwordValidation
) {
}