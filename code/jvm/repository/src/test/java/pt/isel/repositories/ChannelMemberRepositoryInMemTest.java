package pt.isel.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.channels.ChannelMember;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.mem.ChannelMemberRepositoryInMem;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChannelMemberRepositoryInMemTest {

    private ChannelMemberRepositoryInMem repo;
    private UserInfo user1;
    private UserInfo user2;
    private Channel channel1;
    private Channel channel2;

    @BeforeEach
    void setUp() {
        repo = new ChannelMemberRepositoryInMem();
        user1 = new UserInfo(1L, "alice");
        user2 = new UserInfo(2L, "bob");
        channel1 = new Channel(10L, "General", user1, true);
        channel2 = new Channel(20L, "Secret", user1, false);
    }

    @Test
    void testAddAndFindUserInChannel() {
        ChannelMember member = repo.addUserToChannel(user1, channel1, AccessType.READ_WRITE);

        assertNotNull(member);
        assertEquals(1L, member.id());
        assertEquals(user1, member.user());
        assertEquals(channel1, member.channel());
        assertEquals(AccessType.READ_WRITE, member.accessType());

        ChannelMember found = repo.findUserInChannel(user1.id(), channel1.id());
        assertEquals(member, found);
    }

    @Test
    void testFindAllChannelsForUser() {
        repo.addUserToChannel(user1, channel1, AccessType.READ_WRITE);
        repo.addUserToChannel(user1, channel2, AccessType.READ_ONLY);
        repo.addUserToChannel(user2, channel1, AccessType.READ_ONLY);

        List<ChannelMember> aliceChannels = repo.findAllChannelsForUser(user1.id(), 10, 0);
        assertEquals(2, aliceChannels.size());

        List<ChannelMember> bobChannels = repo.findAllChannelsForUser(user2.id(), 10, 0);
        assertEquals(1, bobChannels.size());
    }

    @Test
    void testFindAllMembersInChannel() {
        repo.addUserToChannel(user1, channel1, AccessType.READ_WRITE);
        repo.addUserToChannel(user2, channel1, AccessType.READ_ONLY);

        List<ChannelMember> members = repo.findAllMembersInChannel(channel1.id(), 10, 0);
        assertEquals(2, members.size());
    }

    @Test
    void testRemoveUserFromChannel() {
        repo.addUserToChannel(user1, channel1, AccessType.READ_WRITE);
        assertNotNull(repo.findUserInChannel(user1.id(), channel1.id()));

        repo.removeUserFromChannel(user1.id(), channel1.id());
        assertNull(repo.findUserInChannel(user1.id(), channel1.id()));
    }

    @Test
    void testSaveUpdatesAccessType() {
        ChannelMember member = repo.addUserToChannel(user1, channel1, AccessType.READ_ONLY);
        ChannelMember updated = new ChannelMember(member.id(), user1, channel1, AccessType.READ_WRITE);

        repo.save(updated);

        ChannelMember found = repo.findById(member.id());
        assertEquals(AccessType.READ_WRITE, found.accessType());
    }
}