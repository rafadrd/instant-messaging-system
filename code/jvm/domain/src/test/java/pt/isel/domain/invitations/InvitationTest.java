package pt.isel.domain.invitations;

import org.junit.jupiter.api.Test;
import pt.isel.domain.builders.ChannelBuilder;
import pt.isel.domain.builders.InvitationBuilder;
import pt.isel.domain.builders.UserInfoBuilder;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.users.UserInfo;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class InvitationTest {

    @Test
    void Constructor_ValidArguments_CreatesInstance() {
        UserInfo creator = new UserInfoBuilder().withId(1L).withUsername("Alice").build();
        Channel channel = new ChannelBuilder().withId(1L).withName("Lobby").withOwner(creator).build();
        LocalDateTime expiry = LocalDateTime.now().plusDays(1);

        Invitation inv = new InvitationBuilder()
                .withId(10L)
                .withToken("token123")
                .withCreatedBy(creator)
                .withChannel(channel)
                .withAccessType(AccessType.READ_WRITE)
                .withExpiresAt(expiry)
                .withStatus(InvitationStatus.ACCEPTED)
                .build();

        assertThat(inv.id()).isEqualTo(10L);
        assertThat(inv.token()).isEqualTo("token123");
        assertThat(inv.createdBy()).isEqualTo(creator);
        assertThat(inv.channel()).isEqualTo(channel);
        assertThat(inv.accessType()).isEqualTo(AccessType.READ_WRITE);
        assertThat(inv.expiresAt()).isEqualTo(expiry);
        assertThat(inv.status()).isEqualTo(InvitationStatus.ACCEPTED);
    }

    @Test
    void Constructor_WithoutStatus_SetsStatusToPending() {
        UserInfo creator = new UserInfoBuilder().withId(1L).withUsername("Alice").build();
        Channel channel = new ChannelBuilder().withId(1L).withName("Lobby").withOwner(creator).build();
        LocalDateTime expiry = LocalDateTime.now().plusDays(1);

        Invitation inv = new Invitation(20L, "token456", creator, channel, AccessType.READ_ONLY, expiry);

        assertThat(inv.id()).isEqualTo(20L);
        assertThat(inv.token()).isEqualTo("token456");
        assertThat(inv.accessType()).isEqualTo(AccessType.READ_ONLY);
        assertThat(inv.status()).isEqualTo(InvitationStatus.PENDING);
    }
}