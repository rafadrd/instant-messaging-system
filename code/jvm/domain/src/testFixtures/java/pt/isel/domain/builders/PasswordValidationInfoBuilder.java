package pt.isel.domain.builders;

import pt.isel.domain.security.PasswordValidationInfo;

public class PasswordValidationInfoBuilder {
    private String validationInfo = "hash";

    public PasswordValidationInfoBuilder withValidationInfo(String validationInfo) {
        this.validationInfo = validationInfo;
        return this;
    }

    public PasswordValidationInfo build() {
        return new PasswordValidationInfo(validationInfo);
    }
}