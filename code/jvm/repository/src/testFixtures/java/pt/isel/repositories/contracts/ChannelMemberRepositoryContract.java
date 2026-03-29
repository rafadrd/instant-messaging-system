package pt.isel.repositories.contracts;

import org.junit.jupiter.api.Test;
import pt.isel.domain.builders.ChannelMemberBuilder;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.channels.ChannelMember;
import pt.isel.domain.users.User;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public interface ChannelMemberRepositoryContract extends RepositoryTestHelper {

    @Test
    default void AddUserToChannel_ValidInput_AddsAndFindsUser() {
        getTxManager().run(trx -> {
            User user = insertUser(trx, "alice");
            Channel channel = insertChannel(trx, "General", user, true);

            ChannelMember member = trx.repoMemberships().addUserToChannel(toUserInfo(user), channel, AccessType.READ_WRITE);

            assertThat(member).isNotNull();
            assertThat(member.id()).isNotNull();
            assertThat(member.user().id()).isEqualTo(user.id());
            assertThat(member.channel().id()).isEqualTo(channel.id());
            assertThat(member.accessType()).isEqualTo(AccessType.READ_WRITE);

            ChannelMember found = trx.repoMemberships().findUserInChannel(user.id(), channel.id());
            assertThat(found.id()).isEqualTo(member.id());
            return null;
        });
    }

    @Test
    default void FindAll_HasRecords_ReturnsAllRecords() {
        getTxManager().run(trx -> {
            User user = insertUser(trx, "alice");
            Channel channel1 = insertChannel(trx, "C1", user, true);
            Channel channel2 = insertChannel(trx, "C2", user, true);
            trx.repoMemberships().addUserToChannel(toUserInfo(user), channel1, AccessType.READ_WRITE);
            trx.repoMemberships().addUserToChannel(toUserInfo(user), channel2, AccessType.READ_ONLY);

            List<ChannelMember> allMemberships = trx.repoMemberships().findAll();

            assertThat(allMemberships).hasSize(2);
            return null;
        });
    }

    @Test
    default void FindAllChannelsForUser_ValidUserId_ReturnsChannels() {
        getTxManager().run(trx -> {
            User user1 = insertUser(trx, "alice");
            User user2 = insertUser(trx, "bob");
            Channel channel1 = insertChannel(trx, "General", user1, true);
            Channel channel2 = insertChannel(trx, "Secret", user1, false);
            trx.repoMemberships().addUserToChannel(toUserInfo(user1), channel1, AccessType.READ_WRITE);
            trx.repoMemberships().addUserToChannel(toUserInfo(user1), channel2, AccessType.READ_ONLY);
            trx.repoMemberships().addUserToChannel(toUserInfo(user2), channel1, AccessType.READ_ONLY);

            List<ChannelMember> aliceChannels = trx.repoMemberships().findAllChannelsForUser(user1.id(), 10, 0);
            List<ChannelMember> bobChannels = trx.repoMemberships().findAllChannelsForUser(user2.id(), 10, 0);

            assertThat(aliceChannels).hasSize(2);
            assertThat(bobChannels).hasSize(1);
            return null;
        });
    }

    @Test
    default void FindAllMembersInChannel_ValidChannelId_ReturnsMembers() {
        getTxManager().run(trx -> {
            User user1 = insertUser(trx, "alice");
            User user2 = insertUser(trx, "bob");
            Channel channel = insertChannel(trx, "General", user1, true);
            trx.repoMemberships().addUserToChannel(toUserInfo(user1), channel, AccessType.READ_WRITE);
            trx.repoMemberships().addUserToChannel(toUserInfo(user2), channel, AccessType.READ_ONLY);

            List<ChannelMember> members = trx.repoMemberships().findAllMembersInChannel(channel.id(), 10, 0);

            assertThat(members).hasSize(2);
            return null;
        });
    }

    @Test
    default void RemoveUserFromChannel_ValidInput_RemovesUser() {
        getTxManager().run(trx -> {
            User user = insertUser(trx, "alice");
            Channel channel = insertChannel(trx, "General", user, true);
            trx.repoMemberships().addUserToChannel(toUserInfo(user), channel, AccessType.READ_WRITE);
            assertThat(trx.repoMemberships().findUserInChannel(user.id(), channel.id())).isNotNull();

            trx.repoMemberships().removeUserFromChannel(user.id(), channel.id());

            assertThat(trx.repoMemberships().findUserInChannel(user.id(), channel.id())).isNull();
            return null;
        });
    }

    @Test
    default void Save_UpdatedAccessType_UpdatesRecord() {
        getTxManager().run(trx -> {
            User user = insertUser(trx, "alice");
            Channel channel = insertChannel(trx, "General", user, true);
            ChannelMember member = trx.repoMemberships().addUserToChannel(toUserInfo(user), channel, AccessType.READ_ONLY);
            ChannelMember updated = new ChannelMemberBuilder()
                    .withId(member.id())
                    .withUser(toUserInfo(user))
                    .withChannel(channel)
                    .withAccessType(AccessType.READ_WRITE)
                    .build();

            trx.repoMemberships().save(updated);

            ChannelMember found = trx.repoMemberships().findById(member.id());
            assertThat(found.accessType()).isEqualTo(AccessType.READ_WRITE);
            return null;
        });
    }

    @Test
    default void DeleteById_ValidId_DeletesRecord() {
        getTxManager().run(trx -> {
            User user = insertUser(trx, "alice");
            Channel channel = insertChannel(trx, "General", user, true);
            ChannelMember member = trx.repoMemberships().addUserToChannel(toUserInfo(user), channel, AccessType.READ_WRITE);

            trx.repoMemberships().deleteById(member.id());

            assertThat(trx.repoMemberships().findById(member.id())).isNull();
            return null;
        });
    }

    @Test
    default void Clear_HasRecords_RemovesAllRecords() {
        getTxManager().run(trx -> {
            User user = insertUser(trx, "alice");
            Channel channel = insertChannel(trx, "General", user, true);
            trx.repoMemberships().addUserToChannel(toUserInfo(user), channel, AccessType.READ_WRITE);

            trx.repoMemberships().clear();

            assertThat(trx.repoMemberships().findAll()).isEmpty();
            return null;
        });
    }
}