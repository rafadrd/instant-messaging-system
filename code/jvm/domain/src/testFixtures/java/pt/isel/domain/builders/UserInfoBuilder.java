package pt.isel.domain.builders;

import pt.isel.domain.users.UserInfo;

import java.util.concurrent.atomic.AtomicLong;

public class UserInfoBuilder {
    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);

    private Long id = ID_GENERATOR.getAndIncrement();
    private String username = "testuser_" + id;

    public UserInfoBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public UserInfoBuilder withUsername(String username) {
        this.username = username;
        return this;
    }

    public UserInfo build() {
        return new UserInfo(id, username);
    }
}