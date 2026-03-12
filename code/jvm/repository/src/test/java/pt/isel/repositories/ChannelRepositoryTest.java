package pt.isel.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.User;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.mem.ChannelRepositoryInMem;
import pt.isel.repositories.mem.UserRepositoryInMem;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChannelRepositoryTest {
    private ChannelRepositoryInMem repoChannels;
    private UserInfo ownerInfo;

    @BeforeEach
    void setUp() {
        repoChannels = new ChannelRepositoryInMem();
        UserRepositoryInMem repoUsers = new UserRepositoryInMem();
        User owner = repoUsers.create("AntonioBanderas", new PasswordValidationInfo("pass"));
        ownerInfo = new UserInfo(owner.id(), owner.username());
    }

    @Test
    void create_public_channel() {
        Channel channel = repoChannels.create("Channel1", ownerInfo, true);
        assertEquals(1L, channel.id());
        assertEquals("Channel1", channel.name());
        assertTrue(channel.isPublic());
    }

    @Test
    void find_channel_by_id() {
        Channel created = repoChannels.create("Channel1", ownerInfo, true);
        Channel found = repoChannels.findById(created.id());
        assertEquals(created, found);
    }

    @Test
    void find_channel_by_name() {
        Channel created = repoChannels.create("Channel1", ownerInfo, true);
        Channel found = repoChannels.findByName("Channel1");
        assertEquals(created, found);
    }

    @Test
    void find_all_public_channels() {
        repoChannels.create("Pub1", ownerInfo, true);
        repoChannels.create("Priv1", ownerInfo, false);
        repoChannels.create("Pub2", ownerInfo, true);

        List<Channel> publicChannels = repoChannels.findAllPublicChannels(10, 0);
        assertEquals(2, publicChannels.size());
    }

    @Test
    void find_all_by_owner() {
        repoChannels.create("C1", ownerInfo, true);
        List<Channel> owned = repoChannels.findAllByOwner(ownerInfo.id());
        assertEquals(1, owned.size());
        assertEquals(ownerInfo.id(), owned.getFirst().owner().id());
    }

    @Test
    void delete_channel() {
        Channel c = repoChannels.create("C1", ownerInfo, true);
        repoChannels.deleteById(c.id());
        assertNull(repoChannels.findById(c.id()));
    }
}