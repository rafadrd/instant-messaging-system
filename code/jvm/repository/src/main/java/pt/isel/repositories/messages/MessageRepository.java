package pt.isel.repositories.messages;

import pt.isel.domain.channels.Channel;
import pt.isel.domain.messages.Message;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.Repository;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageRepository extends Repository<Message> {
    Message create(String content, UserInfo user, Channel channel, LocalDateTime createdAt);

    List<Message> findAllInChannel(Channel channel, int limit, int offset);
}