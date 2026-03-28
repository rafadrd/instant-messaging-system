package pt.isel.domain.builders;

import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.User;

import java.util.concurrent.atomic.AtomicLong;

public class UserBuilder {
    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);

    private Long id = ID_GENERATOR.getAndIncrement();
    private String username = "testuser_" + id;
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