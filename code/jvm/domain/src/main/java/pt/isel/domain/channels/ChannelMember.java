package pt.isel.domain.channels;

import pt.isel.domain.users.UserInfo;

public record ChannelMember(
        Long id,
        UserInfo user,
        Channel channel,
        AccessType accessType
) {
}