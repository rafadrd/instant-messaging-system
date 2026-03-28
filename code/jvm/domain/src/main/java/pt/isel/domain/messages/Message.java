package pt.isel.domain.messages;

import pt.isel.domain.channels.Channel;
import pt.isel.domain.users.UserInfo;

import java.time.LocalDateTime;

public record Message(
        Long id,
        String content,
        UserInfo user,
        Channel channel,
        LocalDateTime createdAt
) {
}