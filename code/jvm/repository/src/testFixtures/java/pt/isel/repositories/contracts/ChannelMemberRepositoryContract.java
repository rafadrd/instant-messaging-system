package pt.isel.repositories.contracts;

import org.junit.jupiter.api.Test;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.channels.ChannelMember;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.User;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.TransactionManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public interface ChannelMemberRepositoryContract {
    TransactionManager getTxManager();

    @Test
    default void testAddAndFindUserInChannel() {
        getTxManager().run(trx -> {
            User user = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo userInfo = new UserInfo(user.id(), user.username());
            Channel channel = trx.repoChannels().create("General", userInfo, true);

            ChannelMember member = trx.repoMemberships().addUserToChannel(userInfo, channel, AccessType.READ_WRITE);

            assertNotNull(member);
            assertNotNull(member.id());
            assertEquals(user.id(), member.user().id());
            assertEquals(channel.id(), member.channel().id());
            assertEquals(AccessType.READ_WRITE, member.accessType());

            ChannelMember found = trx.repoMemberships().findUserInChannel(user.id(), channel.id());
            assertEquals(member.id(), found.id());
            return null;
        });
    }

    @Test
    default void testFindAll() {
        getTxManager().run(trx -> {
            User user = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo userInfo = new UserInfo(user.id(), user.username());
            Channel channel1 = trx.repoChannels().create("C1", userInfo, true);
            Channel channel2 = trx.repoChannels().create("C2", userInfo, true);

            trx.repoMemberships().addUserToChannel(userInfo, channel1, AccessType.READ_WRITE);
            trx.repoMemberships().addUserToChannel(userInfo, channel2, AccessType.READ_ONLY);

            List<ChannelMember> allMemberships = trx.repoMemberships().findAll();
            assertEquals(2, allMemberships.size());
            return null;
        });
    }

    @Test
    default void testFindAllChannelsForUser() {
        getTxManager().run(trx -> {
            User user1 = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            User user2 = trx.repoUsers().create("bob", new PasswordValidationInfo("hash"));
            UserInfo u1Info = new UserInfo(user1.id(), user1.username());
            UserInfo u2Info = new UserInfo(user2.id(), user2.username());

            Channel channel1 = trx.repoChannels().create("General", u1Info, true);
            Channel channel2 = trx.repoChannels().create("Secret", u1Info, false);

            trx.repoMemberships().addUserToChannel(u1Info, channel1, AccessType.READ_WRITE);
            trx.repoMemberships().addUserToChannel(u1Info, channel2, AccessType.READ_ONLY);
            trx.repoMemberships().addUserToChannel(u2Info, channel1, AccessType.READ_ONLY);

            List<ChannelMember> aliceChannels = trx.repoMemberships().findAllChannelsForUser(user1.id(), 10, 0);
            assertEquals(2, aliceChannels.size());

            List<ChannelMember> bobChannels = trx.repoMemberships().findAllChannelsForUser(user2.id(), 10, 0);
            assertEquals(1, bobChannels.size());
            return null;
        });
    }

    @Test
    default void testFindAllMembersInChannel() {
        getTxManager().run(trx -> {
            User user1 = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            User user2 = trx.repoUsers().create("bob", new PasswordValidationInfo("hash"));
            UserInfo u1Info = new UserInfo(user1.id(), user1.username());
            UserInfo u2Info = new UserInfo(user2.id(), user2.username());

            Channel channel = trx.repoChannels().create("General", u1Info, true);

            trx.repoMemberships().addUserToChannel(u1Info, channel, AccessType.READ_WRITE);
            trx.repoMemberships().addUserToChannel(u2Info, channel, AccessType.READ_ONLY);

            List<ChannelMember> members = trx.repoMemberships().findAllMembersInChannel(channel.id(), 10, 0);
            assertEquals(2, members.size());
            return null;
        });
    }

    @Test
    default void testRemoveUserFromChannel() {
        getTxManager().run(trx -> {
            User user = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo userInfo = new UserInfo(user.id(), user.username());
            Channel channel = trx.repoChannels().create("General", userInfo, true);

            trx.repoMemberships().addUserToChannel(userInfo, channel, AccessType.READ_WRITE);
            assertNotNull(trx.repoMemberships().findUserInChannel(user.id(), channel.id()));

            trx.repoMemberships().removeUserFromChannel(user.id(), channel.id());
            assertNull(trx.repoMemberships().findUserInChannel(user.id(), channel.id()));
            return null;
        });
    }

    @Test
    default void testSaveUpdatesAccessType() {
        getTxManager().run(trx -> {
            User user = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo userInfo = new UserInfo(user.id(), user.username());
            Channel channel = trx.repoChannels().create("General", userInfo, true);

            ChannelMember member = trx.repoMemberships().addUserToChannel(userInfo, channel, AccessType.READ_ONLY);
            ChannelMember updated = new ChannelMember(member.id(), userInfo, channel, AccessType.READ_WRITE);

            trx.repoMemberships().save(updated);

            ChannelMember found = trx.repoMemberships().findById(member.id());
            assertEquals(AccessType.READ_WRITE, found.accessType());
            return null;
        });
    }

    @Test
    default void testDeleteById() {
        getTxManager().run(trx -> {
            User user = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo userInfo = new UserInfo(user.id(), user.username());
            Channel channel = trx.repoChannels().create("General", userInfo, true);

            ChannelMember member = trx.repoMemberships().addUserToChannel(userInfo, channel, AccessType.READ_WRITE);

            trx.repoMemberships().deleteById(member.id());
            assertNull(trx.repoMemberships().findById(member.id()));
            return null;
        });
    }

    @Test
    default void testClear() {
        getTxManager().run(trx -> {
            User user = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo userInfo = new UserInfo(user.id(), user.username());
            Channel channel = trx.repoChannels().create("General", userInfo, true);

            trx.repoMemberships().addUserToChannel(userInfo, channel, AccessType.READ_WRITE);

            trx.repoMemberships().clear();
            assertTrue(trx.repoMemberships().findAll().isEmpty());
            return null;
        });
    }
}