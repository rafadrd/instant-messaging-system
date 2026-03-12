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

public class ChannelMemberRepositoryTest {
    private ChannelMemberRepositoryInMem repo;
    private UserInfo user;
    private Channel channel;

    @BeforeEach
    void setUp() {
        repo = new ChannelMemberRepositoryInMem();
        user = new UserInfo(1L, "user1");
        user = new UserInfo(1L, "user1");
        channel = new Channel(1L, "channel1", new UserInfo(2L, "owner"), true);
    }

    @Test
    void add_member_to_channel() {
        ChannelMember member = repo.addUserToChannel(user, channel, AccessType.READ_WRITE);
        assertNotNull(member);
        assertEquals(user.id(), member.user().id());
        assertEquals(channel.id(), member.channel().id());
    }

    @Test
    void find_user_in_channel() {
        repo.addUserToChannel(user, channel, AccessType.READ_ONLY);
        ChannelMember found = repo.findUserInChannel(user.id(), channel.id());
        assertNotNull(found);
        assertEquals(AccessType.READ_ONLY, found.accessType());
    }

    @Test
    void remove_user_from_channel() {
        repo.addUserToChannel(user, channel, AccessType.READ_WRITE);
        repo.removeUserFromChannel(user.id(), channel.id());
        assertNull(repo.findUserInChannel(user.id(), channel.id()));
    }

    @Test
    void find_all_channels_for_user() {
        Channel channel2 = new Channel(2L, "c2", user, true);
        repo.addUserToChannel(user, channel, AccessType.READ_ONLY);
        repo.addUserToChannel(user, channel2, AccessType.READ_ONLY);

        List<ChannelMember> memberships = repo.findAllChannelsForUser(user.id(), 10, 0);
        assertEquals(2, memberships.size());
    }

    @Test
    void find_all_members_in_channel() {
        UserInfo user2 = new UserInfo(3L, "user2");
        repo.addUserToChannel(user, channel, AccessType.READ_ONLY);
        repo.addUserToChannel(user2, channel, AccessType.READ_WRITE);

        List<ChannelMember> members = repo.findAllMembersInChannel(channel.id(), 10, 0);
        assertEquals(2, members.size());
    }
}