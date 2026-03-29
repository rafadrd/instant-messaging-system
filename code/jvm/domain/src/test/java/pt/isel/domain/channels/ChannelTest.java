package pt.isel.domain.channels;

import org.junit.jupiter.api.Test;
import pt.isel.domain.builders.ChannelBuilder;
import pt.isel.domain.builders.UserInfoBuilder;
import pt.isel.domain.users.UserInfo;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelTest {

    @Test
    void Constructor_ValidArguments_CreatesInstance() {
        UserInfo owner = new UserInfoBuilder().withId(1L).withUsername("Alice").build();

        Channel channel = new ChannelBuilder()
                .withId(10L)
                .withName("Secret Room")
                .withOwner(owner)
                .withIsPublic(false)
                .build();

        assertThat(channel.id()).isEqualTo(10L);
        assertThat(channel.name()).isEqualTo("Secret Room");
        assertThat(channel.owner()).isEqualTo(owner);
        assertThat(channel.isPublic()).isFalse();
    }

    @Test
    void Constructor_WithoutIsPublic_SetsPublicToTrue() {
        UserInfo owner = new UserInfoBuilder().withId(2L).withUsername("Bob").build();

        Channel channel = new Channel(20L, "Public Lobby", owner);

        assertThat(channel.id()).isEqualTo(20L);
        assertThat(channel.name()).isEqualTo("Public Lobby");
        assertThat(channel.owner()).isEqualTo(owner);
        assertThat(channel.isPublic()).isTrue();
    }
}