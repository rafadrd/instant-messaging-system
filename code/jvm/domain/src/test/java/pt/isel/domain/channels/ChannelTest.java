package pt.isel.domain.channels;

import org.junit.jupiter.api.Test;
import pt.isel.domain.users.UserInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelTest {

    @Test
    void testMainConstructor() {
        UserInfo owner = new UserInfo(1L, "Alice");
        Channel channel = new Channel(10L, "Secret Room", owner, false);

        assertEquals(10L, channel.id());
        assertEquals("Secret Room", channel.name());
        assertEquals(owner, channel.owner());
        assertFalse(channel.isPublic());
    }

    @Test
    void testAuxiliaryConstructorSetsPublicToTrue() {
        UserInfo owner = new UserInfo(2L, "Bob");
        Channel channel = new Channel(20L, "Public Lobby", owner);

        assertEquals(20L, channel.id());
        assertEquals("Public Lobby", channel.name());
        assertEquals(owner, channel.owner());
        assertTrue(channel.isPublic());
    }
}