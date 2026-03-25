package pt.isel.domain.messages;

import org.junit.jupiter.api.Test;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.users.UserInfo;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageTest {

    @Test
    void testMainConstructor() {
        UserInfo user = new UserInfo(1L, "Alice");
        Channel channel = new Channel(1L, "Lobby", user);
        LocalDateTime fixedTime = LocalDateTime.of(2025, 1, 1, 12, 0);

        Message msg = new Message(100L, "Hello World", user, channel, fixedTime);

        assertEquals(100L, msg.id());
        assertEquals("Hello World", msg.content());
        assertEquals(user, msg.user());
        assertEquals(channel, msg.channel());
        assertEquals(fixedTime, msg.createdAt());
    }

    @Test
    void testAuxiliaryConstructorSetsCreatedAtToNow() {
        UserInfo user = new UserInfo(2L, "Bob");
        Channel channel = new Channel(2L, "General", user);

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        Message msg = new Message(200L, "What's up?", user, channel);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertEquals(200L, msg.id());
        assertEquals("What's up?", msg.content());

        assertNotNull(msg.createdAt());
        assertTrue(msg.createdAt().isAfter(before) && msg.createdAt().isBefore(after));
    }
}