package pt.isel.domain.security;

import jakarta.inject.Named;

@Named
public class PasswordSecurityDomain {
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyConfig passwordPolicyConfig;

    public PasswordSecurityDomain(PasswordEncoder passwordEncoder, PasswordPolicyConfig passwordPolicyConfig) {
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyConfig = passwordPolicyConfig;
    }

    public boolean validatePassword(String password, PasswordValidationInfo validationInfo) {
        return passwordEncoder.matches(password, validationInfo.validationInfo());
    }

    public PasswordValidationInfo createPasswordValidationInformation(String password) {
        return new PasswordValidationInfo(passwordEncoder.encode(password));
    }

    public boolean isSafePassword(String password) {
        if (password.length() < passwordPolicyConfig.minLength()) return false;

        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }

        if (passwordPolicyConfig.requiresUppercase() && !hasUpper) return false;
        if (passwordPolicyConfig.requiresLowercase() && !hasLower) return false;
        if (passwordPolicyConfig.requiresDigit() && !hasDigit) return false;
        return !passwordPolicyConfig.requiresSpecialChar() || hasSpecial;
    }
}