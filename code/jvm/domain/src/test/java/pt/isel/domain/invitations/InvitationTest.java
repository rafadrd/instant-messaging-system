package pt.isel.domain.invitations;

import org.junit.jupiter.api.Test;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.users.UserInfo;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvitationTest {

    @Test
    void testMainConstructor() {
        UserInfo creator = new UserInfo(1L, "Alice");
        Channel channel = new Channel(1L, "Lobby", creator);
        LocalDateTime expiry = LocalDateTime.now().plusDays(1);

        Invitation inv = new Invitation(10L, "token123", creator, channel, AccessType.READ_WRITE, expiry, InvitationStatus.ACCEPTED);

        assertEquals(10L, inv.id());
        assertEquals("token123", inv.token());
        assertEquals(creator, inv.createdBy());
        assertEquals(channel, inv.channel());
        assertEquals(AccessType.READ_WRITE, inv.accessType());
        assertEquals(expiry, inv.expiresAt());
        assertEquals(InvitationStatus.ACCEPTED, inv.status());
    }

    @Test
    void testAuxiliaryConstructorSetsStatusToPending() {
        UserInfo creator = new UserInfo(1L, "Alice");
        Channel channel = new Channel(1L, "Lobby", creator);
        LocalDateTime expiry = LocalDateTime.now().plusDays(1);

        Invitation inv = new Invitation(20L, "token456", creator, channel, AccessType.READ_ONLY, expiry);

        assertEquals(20L, inv.id());
        assertEquals("token456", inv.token());
        assertEquals(AccessType.READ_ONLY, inv.accessType());
        assertEquals(InvitationStatus.PENDING, inv.status());
    }
}