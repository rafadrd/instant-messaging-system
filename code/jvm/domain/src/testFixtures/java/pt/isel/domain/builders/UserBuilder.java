package pt.isel.domain.builders;

import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.User;

public class UserBuilder {
    private Long id = 1L;
    private String username = "testuser";
    private PasswordValidationInfo passwordValidation = new PasswordValidationInfo("hash");

    public UserBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public UserBuilder withUsername(String username) {
        this.username = username;
        return this;
    }

    public UserBuilder withPasswordValidation(PasswordValidationInfo passwordValidation) {
        this.passwordValidation = passwordValidation;
        return this;
    }

    public User build() {
        return new User(id, username, passwordValidation);
    }
}