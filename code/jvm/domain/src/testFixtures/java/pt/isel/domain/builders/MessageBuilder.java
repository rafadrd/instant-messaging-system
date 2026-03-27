package pt.isel.domain.builders;

import pt.isel.domain.channels.Channel;
import pt.isel.domain.messages.Message;
import pt.isel.domain.users.UserInfo;

import java.time.LocalDateTime;

public class MessageBuilder {
    private Long id = 1000L;
    private String content = "Hello World";
    private UserInfo user = new UserInfoBuilder().build();
    private Channel channel = new ChannelBuilder().build();
    private LocalDateTime createdAt = LocalDateTime.of(2025, 1, 1, 12, 0);

    public MessageBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public MessageBuilder withContent(String content) {
        this.content = content;
        return this;
    }

    public MessageBuilder withUser(UserInfo user) {
        this.user = user;
        return this;
    }

    public MessageBuilder withChannel(Channel channel) {
        this.channel = channel;
        return this;
    }

    public MessageBuilder withCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Message build() {
        return new Message(id, content, user, channel, createdAt);
    }
}