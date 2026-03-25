package pt.isel.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.invitations.Invitation;
import pt.isel.domain.invitations.InvitationStatus;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.mem.InvitationRepositoryInMem;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvitationRepositoryInMemTest {

    private InvitationRepositoryInMem repo;
    private UserInfo creator;
    private Channel channel;

    @BeforeEach
    void setUp() {
        repo = new InvitationRepositoryInMem();
        creator = new UserInfo(1L, "alice");
        channel = new Channel(10L, "Secret", creator, false);
    }

    @Test
    void testCreateAndFindByToken() {
        LocalDateTime expiry = LocalDateTime.now().plusDays(1);
        Invitation inv = repo.create("token-123", creator, channel, AccessType.READ_ONLY, expiry);

        assertNotNull(inv);
        assertEquals(1L, inv.id());
        assertEquals("token-123", inv.token());
        assertEquals(InvitationStatus.PENDING, inv.status());

        Invitation found = repo.findByToken("token-123");
        assertEquals(inv, found);
    }

    @Test
    void testFindByChannelId() {
        repo.create("t1", creator, channel, AccessType.READ_ONLY, LocalDateTime.now().plusDays(1));
        repo.create("t2", creator, channel, AccessType.READ_WRITE, LocalDateTime.now().plusDays(1));

        List<Invitation> invs = repo.findByChannelId(channel.id());
        assertEquals(2, invs.size());
    }

    @Test
    void testConsumeInvitation() {
        Invitation inv = repo.create("t1", creator, channel, AccessType.READ_ONLY, LocalDateTime.now().plusDays(1));

        assertTrue(repo.consume(inv.id()));

        Invitation updated = repo.findById(inv.id());
        assertEquals(InvitationStatus.ACCEPTED, updated.status());

        assertFalse(repo.consume(inv.id()));
    }

    @Test
    void testSaveUpdatesInvitation() {
        Invitation inv = repo.create("t1", creator, channel, AccessType.READ_ONLY, LocalDateTime.now().plusDays(1));
        Invitation rejected = new Invitation(inv.id(), inv.token(), inv.createdBy(), inv.channel(), inv.accessType(), inv.expiresAt(), InvitationStatus.REJECTED);

        repo.save(rejected);

        assertEquals(InvitationStatus.REJECTED, repo.findById(inv.id()).status());
    }
}