package pt.isel.domain.messages;

import org.junit.jupiter.api.Test;
import pt.isel.domain.builders.ChannelBuilder;
import pt.isel.domain.builders.MessageBuilder;
import pt.isel.domain.builders.UserInfoBuilder;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.users.UserInfo;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTest {

    @Test
    void Constructor_ValidArguments_CreatesInstance() {
        UserInfo user = new UserInfoBuilder().withId(1L).withUsername("Alice").build();
        Channel channel = new ChannelBuilder().withId(1L).withName("Lobby").withOwner(user).build();
        LocalDateTime fixedTime = LocalDateTime.of(2025, 1, 1, 12, 0);

        Message msg = new MessageBuilder()
                .withId(100L)
                .withContent("Hello World")
                .withUser(user)
                .withChannel(channel)
                .withCreatedAt(fixedTime)
                .build();

        assertThat(msg.id()).isEqualTo(100L);
        assertThat(msg.content()).isEqualTo("Hello World");
        assertThat(msg.user()).isEqualTo(user);
        assertThat(msg.channel()).isEqualTo(channel);
        assertThat(msg.createdAt()).isEqualTo(fixedTime);
    }
}