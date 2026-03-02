package pt.isel.domain.security;

public record PasswordPolicyConfig(
        int minLength,
        boolean requiresUppercase,
        boolean requiresLowercase,
        boolean requiresDigit,
        boolean requiresSpecialChar
) {
}