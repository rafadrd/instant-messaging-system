package pt.isel.domain.builders;

import pt.isel.domain.users.UserInfo;

public class UserInfoBuilder {
    private Long id = 1L;
    private String username = "testuser";

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