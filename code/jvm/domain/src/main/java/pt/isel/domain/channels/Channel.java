package pt.isel.domain.channels;

import pt.isel.domain.users.UserInfo;

public record Channel(
        Long id,
        String name,
        UserInfo owner,
        boolean isPublic
) {
    public Channel(Long id, String name, UserInfo owner) {
        this(id, name, owner, true);
    }
}