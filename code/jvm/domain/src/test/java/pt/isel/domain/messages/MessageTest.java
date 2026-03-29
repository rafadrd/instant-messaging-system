package pt.isel.domain.messages;

import org.junit.jupiter.api.Test;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.users.UserInfo;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTest {

    @Test
    void Constructor_ValidArguments_CreatesInstance() {
        UserInfo user = new UserInfo(1L, "Alice");
        Channel channel = new Channel(1L, "Lobby", user);
        LocalDateTime fixedTime = LocalDateTime.of(2025, 1, 1, 12, 0);

        Message msg = new Message(100L, "Hello World", user, channel, fixedTime);

        assertThat(msg.id()).isEqualTo(100L);
        assertThat(msg.content()).isEqualTo("Hello World");
        assertThat(msg.user()).isEqualTo(user);
        assertThat(msg.channel()).isEqualTo(channel);
        assertThat(msg.createdAt()).isEqualTo(fixedTime);
    }
}