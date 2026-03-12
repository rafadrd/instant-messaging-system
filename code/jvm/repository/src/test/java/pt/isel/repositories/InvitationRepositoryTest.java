package pt.isel.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.invitations.Invitation;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.mem.InvitationRepositoryInMem;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class InvitationRepositoryTest {
    private InvitationRepositoryInMem repo;
    private UserInfo creator;
    private Channel channel;

    @BeforeEach
    void setUp() {
        repo = new InvitationRepositoryInMem();
        creator = new UserInfo(1L, "creator");
        channel = new Channel(1L, "channel", creator, false);
    }

    @Test
    void create_invitation() {
        String token = "secret-token";
        LocalDateTime expiry = LocalDateTime.now().plusDays(1);
        Invitation inv = repo.create(token, creator, channel, AccessType.READ_ONLY, expiry);

        assertEquals(token, inv.token());
        assertEquals(channel.id(), inv.channel().id());
        assertEquals(AccessType.READ_ONLY, inv.accessType());
    }

    @Test
    void find_by_token() {
        repo.create("t1", creator, channel, AccessType.READ_WRITE, LocalDateTime.now().plusDays(1));
        Invitation found = repo.findByToken("t1");
        assertNotNull(found);
        assertEquals("t1", found.token());
    }

    @Test
    void find_by_channel_id() {
        repo.create("t1", creator, channel, AccessType.READ_WRITE, LocalDateTime.now().plusDays(1));
        List<Invitation> list = repo.findByChannelId(channel.id());
        assertEquals(1, list.size());
    }

    @Test
    void delete_invitation() {
        Invitation inv = repo.create("t1", creator, channel, AccessType.READ_WRITE, LocalDateTime.now().plusDays(1));
        repo.deleteById(inv.id());
        assertNull(repo.findById(inv.id()));
    }
}