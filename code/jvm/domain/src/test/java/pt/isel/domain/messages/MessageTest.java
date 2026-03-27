package pt.isel.domain.messages;

import org.junit.jupiter.api.Test;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.users.UserInfo;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTest {

    @Test
    void testMainConstructor() {
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

    @Test
    void testAuxiliaryConstructorSetsCreatedAtToNow() {
        UserInfo user = new UserInfo(2L, "Bob");
        Channel channel = new Channel(2L, "General", user);
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        Message msg = new Message(200L, "What's up?", user, channel);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertThat(msg.id()).isEqualTo(200L);
        assertThat(msg.content()).isEqualTo("What's up?");
        assertThat(msg.createdAt()).isNotNull().isAfter(before).isBefore(after);
    }
}