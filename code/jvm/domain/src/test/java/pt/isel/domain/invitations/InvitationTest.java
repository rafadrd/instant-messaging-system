package pt.isel.domain.invitations;

import org.junit.jupiter.api.Test;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.users.UserInfo;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class InvitationTest {

    @Test
    void Constructor_ValidArguments_CreatesInstance() {
        UserInfo creator = new UserInfo(1L, "Alice");
        Channel channel = new Channel(1L, "Lobby", creator);
        LocalDateTime expiry = LocalDateTime.now().plusDays(1);

        Invitation inv = new Invitation(10L, "token123", creator, channel, AccessType.READ_WRITE, expiry, InvitationStatus.ACCEPTED);

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
        UserInfo creator = new UserInfo(1L, "Alice");
        Channel channel = new Channel(1L, "Lobby", creator);
        LocalDateTime expiry = LocalDateTime.now().plusDays(1);

        Invitation inv = new Invitation(20L, "token456", creator, channel, AccessType.READ_ONLY, expiry);

        assertThat(inv.id()).isEqualTo(20L);
        assertThat(inv.token()).isEqualTo("token456");
        assertThat(inv.accessType()).isEqualTo(AccessType.READ_ONLY);
        assertThat(inv.status()).isEqualTo(InvitationStatus.PENDING);
    }
}