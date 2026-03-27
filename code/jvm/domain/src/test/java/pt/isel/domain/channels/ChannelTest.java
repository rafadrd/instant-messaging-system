package pt.isel.domain.channels;

import org.junit.jupiter.api.Test;
import pt.isel.domain.users.UserInfo;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelTest {

    @Test
    void testMainConstructor() {
        UserInfo owner = new UserInfo(1L, "Alice");
        Channel channel = new Channel(10L, "Secret Room", owner, false);

        assertThat(channel.id()).isEqualTo(10L);
        assertThat(channel.name()).isEqualTo("Secret Room");
        assertThat(channel.owner()).isEqualTo(owner);
        assertThat(channel.isPublic()).isFalse();
    }

    @Test
    void testAuxiliaryConstructorSetsPublicToTrue() {
        UserInfo owner = new UserInfo(2L, "Bob");
        Channel channel = new Channel(20L, "Public Lobby", owner);

        assertThat(channel.id()).isEqualTo(20L);
        assertThat(channel.name()).isEqualTo("Public Lobby");
        assertThat(channel.owner()).isEqualTo(owner);
        assertThat(channel.isPublic()).isTrue();
    }
}