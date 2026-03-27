package pt.isel.services.channels;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.isel.domain.builders.UserInfoBuilder;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.channels.ChannelMember;
import pt.isel.domain.common.ChannelError;
import pt.isel.domain.common.Either;
import pt.isel.domain.common.MessageError;
import pt.isel.domain.invitations.Invitation;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.User;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.mem.TransactionManagerInMem;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelServiceTest {

    private TransactionManagerInMem trxManager;
    private ChannelService channelService;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        trxManager = new TransactionManagerInMem();
        channelService = new ChannelService(trxManager);

        alice = trxManager.run(trx -> trx.repoUsers().create("alice", new PasswordValidationInfo("hash")));
        bob = trxManager.run(trx -> trx.repoUsers().create("bob", new PasswordValidationInfo("hash")));
    }

    @Test
    void testCreateChannel_Success() {
        Either<ChannelError, Channel> result = channelService.createChannel("General", alice.id(), true);

        assertThat(result).isInstanceOf(Either.Right.class);
        Channel channel = ((Either.Right<ChannelError, Channel>) result).value();
        assertThat(channel.name()).isEqualTo("General");
        assertThat(channel.owner().id()).isEqualTo(alice.id());
        assertThat(channel.isPublic()).isTrue();

        trxManager.run(trx -> {
            ChannelMember member = trx.repoMemberships().findUserInChannel(alice.id(), channel.id());
            assertThat(member).isNotNull();
            assertThat(member.accessType()).isEqualTo(AccessType.READ_WRITE);
            return null;
        });
    }

    @Test
    void testCreateChannel_ChannelAlreadyExists() {
        channelService.createChannel("General", alice.id(), true);
        Either<ChannelError, Channel> result = channelService.createChannel("General", bob.id(), true);

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, Channel>) result).value()).isInstanceOf(ChannelError.ChannelAlreadyExists.class);
    }

    @Test
    void testCreateChannel_EmptyName() {
        assertThat(channelService.createChannel("", alice.id(), true)).isInstanceOf(Either.Left.class);
        assertThat(channelService.createChannel(null, alice.id(), true)).isInstanceOf(Either.Left.class);
    }

    @Test
    void testCreateChannel_NameTooLong() {
        String longName = "a".repeat(31);
        Either<ChannelError, Channel> result = channelService.createChannel(longName, alice.id(), true);

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, Channel>) result).value()).isInstanceOf(ChannelError.InvalidChannelNameLength.class);
    }

    @Test
    void testCreateChannel_UserNotFound() {
        Either<ChannelError, Channel> result = channelService.createChannel("General", 999L, true);

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, Channel>) result).value()).isInstanceOf(ChannelError.UserNotFound.class);
    }

    @Test
    void testGetChannelById_Success() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        Either<ChannelError, Channel> result = channelService.getChannelById(created.id());

        assertThat(result).isInstanceOf(Either.Right.class);
        assertThat(((Either.Right<ChannelError, Channel>) result).value().id()).isEqualTo(created.id());
    }

    @Test
    void testGetChannelById_NotFound() {
        Either<ChannelError, Channel> result = channelService.getChannelById(999L);

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, Channel>) result).value()).isInstanceOf(ChannelError.ChannelNotFound.class);
    }

    @Test
    void testDeleteChannel_Success() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        Either<ChannelError, String> result = channelService.deleteChannel(alice.id(), created.id());

        assertThat(result).isInstanceOf(Either.Right.class);
        assertThat(channelService.getChannelById(created.id())).isInstanceOf(Either.Left.class);
    }

    @Test
    void testDeleteChannel_NotOwner() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        Either<ChannelError, String> result = channelService.deleteChannel(bob.id(), created.id());

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, String>) result).value()).isInstanceOf(ChannelError.UserNotOwner.class);
    }

    @Test
    void testGetJoinedChannels_Success() {
        channelService.createChannel("C1", alice.id(), true);
        channelService.createChannel("C2", alice.id(), true);

        Either<ChannelError, List<Channel>> result = channelService.getJoinedChannels(alice.id(), 10, 0);

        assertThat(result).isInstanceOf(Either.Right.class);
        assertThat(((Either.Right<ChannelError, List<Channel>>) result).value()).hasSize(2);
    }

    @Test
    void testEditChannel_Success() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        Either<ChannelError, Channel> result = channelService.editChannel(alice.id(), created.id(), "NewName", false);

        assertThat(result).isInstanceOf(Either.Right.class);
        Channel updated = ((Either.Right<ChannelError, Channel>) result).value();
        assertThat(updated.name()).isEqualTo("NewName");
        assertThat(updated.isPublic()).isFalse();
    }

    @Test
    void testEditChannel_ChannelAlreadyExists() {
        Channel c1 = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        channelService.createChannel("Random", alice.id(), true);

        Either<ChannelError, Channel> result = channelService.editChannel(alice.id(), c1.id(), "Random", true);

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, Channel>) result).value()).isInstanceOf(ChannelError.ChannelAlreadyExists.class);
    }

    @Test
    void testEditChannel_UserNotFound() {
        Channel c1 = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        Either<ChannelError, Channel> result = channelService.editChannel(999L, c1.id(), "NewName", true);

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, Channel>) result).value()).isInstanceOf(ChannelError.UserNotFound.class);
    }

    @Test
    void testEditChannel_ChannelNotFound() {
        Either<ChannelError, Channel> result = channelService.editChannel(alice.id(), 999L, "NewName", true);

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, Channel>) result).value()).isInstanceOf(ChannelError.ChannelNotFound.class);
    }

    @Test
    void testGetAccessType_Success() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        channelService.joinPublicChannel(bob.id(), created.id());

        Either<MessageError, AccessType> result = channelService.getAccessType(alice.id(), bob.id(), created.id());

        assertThat(result).isInstanceOf(Either.Right.class);
        assertThat(((Either.Right<MessageError, AccessType>) result).value()).isEqualTo(AccessType.READ_WRITE);
    }

    @Test
    void testEditMemberAccess_Success() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        channelService.joinPublicChannel(bob.id(), created.id());

        Either<ChannelError, ChannelMember> result = channelService.editMemberAccess(alice.id(), created.id(), bob.id(), AccessType.READ_ONLY);

        assertThat(result).isInstanceOf(Either.Right.class);
        assertThat(((Either.Right<ChannelError, ChannelMember>) result).value().accessType()).isEqualTo(AccessType.READ_ONLY);
    }

    @Test
    void testEditMemberAccess_TargetIsOwner() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        Either<ChannelError, ChannelMember> result = channelService.editMemberAccess(alice.id(), created.id(), alice.id(), AccessType.READ_ONLY);

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, ChannelMember>) result).value()).isInstanceOf(ChannelError.UserIsOwner.class);
    }

    @Test
    void testEditMemberAccess_TargetNotInChannel() {
        Channel c1 = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();

        Either<ChannelError, ChannelMember> result = channelService.editMemberAccess(alice.id(), c1.id(), bob.id(), AccessType.READ_WRITE);

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, ChannelMember>) result).value()).isInstanceOf(ChannelError.UserNotInChannel.class);
    }

    @Test
    void testSearchChannels_Success() {
        channelService.createChannel("Java Devs", alice.id(), true);
        channelService.createChannel("Secret", alice.id(), false);

        Either<ChannelError, List<Channel>> result = channelService.searchChannels("java", 10, 0);

        assertThat(result).isInstanceOf(Either.Right.class);
        List<Channel> channels = ((Either.Right<ChannelError, List<Channel>>) result).value();
        assertThat(channels).hasSize(1);
        assertThat(channels.getFirst().name()).isEqualTo("Java Devs");
    }

    @Test
    void testJoinPublicChannel_Success() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        Either<ChannelError, String> result = channelService.joinPublicChannel(bob.id(), created.id());

        assertThat(result).isInstanceOf(Either.Right.class);
    }

    @Test
    void testJoinPublicChannel_PrivateChannel() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("Secret", alice.id(), false)).value();
        Either<ChannelError, String> result = channelService.joinPublicChannel(bob.id(), created.id());

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, String>) result).value()).isInstanceOf(ChannelError.ChannelIsPrivate.class);
    }

    @Test
    void testJoinPrivateChannel_Success() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("Secret", alice.id(), false)).value();

        Invitation inv = trxManager.run(trx -> trx.repoInvitations().create(
                "token123", new UserInfoBuilder().withId(alice.id()).withUsername(alice.username()).build(), created, AccessType.READ_ONLY, LocalDateTime.now(ZoneOffset.UTC).plusDays(1)
        ));

        Either<ChannelError, String> result = channelService.joinPrivateChannel(bob.id(), inv.token());
        assertThat(result).isInstanceOf(Either.Right.class);

        Either<MessageError, AccessType> access = channelService.getAccessType(bob.id(), bob.id(), created.id());
        assertThat(((Either.Right<MessageError, AccessType>) access).value()).isEqualTo(AccessType.READ_ONLY);
    }

    @Test
    void testLeaveChannel_Success() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        channelService.joinPublicChannel(bob.id(), created.id());

        Either<ChannelError, String> result = channelService.leaveChannel(created.id(), bob.id());
        assertThat(result).isInstanceOf(Either.Right.class);
    }

    @Test
    void testLeaveChannel_OwnerCannotLeave() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        Either<ChannelError, String> result = channelService.leaveChannel(created.id(), alice.id());

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, String>) result).value()).isInstanceOf(ChannelError.OwnerCannotLeave.class);
    }

    @Test
    void testDeleteChannel_UserNotFound() {
        Either<ChannelError, String> result = channelService.deleteChannel(999L, 1L);

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, String>) result).value()).isInstanceOf(ChannelError.UserNotFound.class);
    }

    @Test
    void testDeleteChannel_ChannelNotFound() {
        Either<ChannelError, String> result = channelService.deleteChannel(alice.id(), 999L);

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, String>) result).value()).isInstanceOf(ChannelError.ChannelNotFound.class);
    }

    @Test
    void testGetJoinedChannels_UserNotFound() {
        Either<ChannelError, List<Channel>> result = channelService.getJoinedChannels(999L, 10, 0);

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, List<Channel>>) result).value()).isInstanceOf(ChannelError.UserNotFound.class);
    }

    @Test
    void testEditChannel_EmptyName() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        assertThat(channelService.editChannel(alice.id(), created.id(), "", true)).isInstanceOf(Either.Left.class);
    }

    @Test
    void testEditChannel_NotOwner() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        Either<ChannelError, Channel> result = channelService.editChannel(bob.id(), created.id(), "NewName", true);

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, Channel>) result).value()).isInstanceOf(ChannelError.UserNotOwner.class);
    }

    @Test
    void testGetAccessType_TargetNotInChannel() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        Either<MessageError, AccessType> result = channelService.getAccessType(alice.id(), bob.id(), created.id());

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<MessageError, AccessType>) result).value()).isInstanceOf(MessageError.UserNotInChannel.class);
    }

    @Test
    void testGetAccessType_RequesterNotAuthorized() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        channelService.joinPublicChannel(bob.id(), created.id());

        User charlie = trxManager.run(trx -> trx.repoUsers().create("charlie", new PasswordValidationInfo("hash")));

        Either<MessageError, AccessType> result = channelService.getAccessType(charlie.id(), bob.id(), created.id());

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<MessageError, AccessType>) result).value()).isInstanceOf(MessageError.UserNotAuthorized.class);
    }

    @Test
    void testEditMemberAccess_NotOwner() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        channelService.joinPublicChannel(bob.id(), created.id());

        Either<ChannelError, ChannelMember> result = channelService.editMemberAccess(bob.id(), created.id(), bob.id(), AccessType.READ_ONLY);

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, ChannelMember>) result).value()).isInstanceOf(ChannelError.UserNotOwner.class);
    }

    @Test
    void testSearchChannels_EmptyQuery() {
        channelService.createChannel("Java Devs", alice.id(), true);
        channelService.createChannel("Secret", alice.id(), false);

        Either<ChannelError, List<Channel>> result = channelService.searchChannels("", 10, 0);

        assertThat(result).isInstanceOf(Either.Right.class);
        assertThat(((Either.Right<ChannelError, List<Channel>>) result).value()).hasSize(1);
    }

    @Test
    void testJoinPublicChannel_AlreadyInChannel() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        channelService.joinPublicChannel(bob.id(), created.id());

        Either<ChannelError, String> result = channelService.joinPublicChannel(bob.id(), created.id());

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, String>) result).value()).isInstanceOf(ChannelError.UserAlreadyInChannel.class);
    }

    @Test
    void testJoinPrivateChannel_TokenNotFound() {
        Either<ChannelError, String> result = channelService.joinPrivateChannel(bob.id(), "invalid-token");

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, String>) result).value()).isInstanceOf(ChannelError.TokenNotFound.class);
    }

    @Test
    void testJoinPrivateChannel_ExpiredToken() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("Secret", alice.id(), false)).value();
        trxManager.run(trx -> trx.repoInvitations().create(
                "expired-token", new UserInfoBuilder().withId(alice.id()).withUsername(alice.username()).build(), created, AccessType.READ_ONLY, LocalDateTime.now(ZoneOffset.UTC).minusDays(1)
        ));

        Either<ChannelError, String> result = channelService.joinPrivateChannel(bob.id(), "expired-token");

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, String>) result).value()).isInstanceOf(ChannelError.InvitationExpired.class);
    }

    @Test
    void testLeaveChannel_ChannelNotFound() {
        Either<ChannelError, String> result = channelService.leaveChannel(999L, bob.id());

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, String>) result).value()).isInstanceOf(ChannelError.ChannelNotFound.class);
    }

    @Test
    void testLeaveChannel_UserNotInChannel() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        Either<ChannelError, String> result = channelService.leaveChannel(created.id(), bob.id());

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, String>) result).value()).isInstanceOf(ChannelError.UserNotInChannel.class);
    }

    @Test
    void testGetUsersInChannel_Success() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        channelService.joinPublicChannel(bob.id(), created.id());

        Either<ChannelError, List<UserInfo>> result = channelService.getUsersInChannel(created.id(), 10, 0);

        assertThat(result).isInstanceOf(Either.Right.class);
        assertThat(((Either.Right<ChannelError, List<UserInfo>>) result).value()).hasSize(2);
    }

    @Test
    void testEditChannel_NameTooLong() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        String longName = "a".repeat(31);

        Either<ChannelError, Channel> result = channelService.editChannel(alice.id(), created.id(), longName, true);

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, Channel>) result).value()).isInstanceOf(ChannelError.InvalidChannelNameLength.class);
    }

    @Test
    void testEditMemberAccess_UserNotFound() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();

        Either<ChannelError, ChannelMember> result = channelService.editMemberAccess(alice.id(), created.id(), 999L, AccessType.READ_ONLY);

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, ChannelMember>) result).value()).isInstanceOf(ChannelError.UserNotFound.class);
    }

    @Test
    void testJoinPublicChannel_UserNotFound() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();

        Either<ChannelError, String> result = channelService.joinPublicChannel(999L, created.id());

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, String>) result).value()).isInstanceOf(ChannelError.UserNotFound.class);
    }

    @Test
    void testJoinPublicChannel_ChannelNotFound() {
        Either<ChannelError, String> result = channelService.joinPublicChannel(bob.id(), 999L);

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, String>) result).value()).isInstanceOf(ChannelError.ChannelNotFound.class);
    }

    @Test
    void testJoinPrivateChannel_UserNotFound() {
        Either<ChannelError, String> result = channelService.joinPrivateChannel(999L, "some-token");

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, String>) result).value()).isInstanceOf(ChannelError.UserNotFound.class);
    }

    @Test
    void testJoinPrivateChannel_AlreadyInChannel() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("Secret", alice.id(), false)).value();
        trxManager.run(trx -> trx.repoMemberships().addUserToChannel(new UserInfoBuilder().withId(bob.id()).withUsername(bob.username()).build(), created, AccessType.READ_ONLY));
        trxManager.run(trx -> trx.repoInvitations().create(
                "token123", new UserInfoBuilder().withId(alice.id()).withUsername(alice.username()).build(), created, AccessType.READ_ONLY, LocalDateTime.now(ZoneOffset.UTC).plusDays(1)
        ));

        Either<ChannelError, String> result = channelService.joinPrivateChannel(bob.id(), "token123");

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, String>) result).value()).isInstanceOf(ChannelError.UserAlreadyInChannel.class);
    }

    @Test
    void testJoinPrivateChannel_InvitationAlreadyUsed() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("Secret", alice.id(), false)).value();
        trxManager.run(trx -> trx.repoInvitations().create(
                "token123", new UserInfoBuilder().withId(alice.id()).withUsername(alice.username()).build(), created, AccessType.READ_ONLY, LocalDateTime.now(ZoneOffset.UTC).plusDays(1)
        ));

        channelService.joinPrivateChannel(bob.id(), "token123");

        User charlie = trxManager.run(trx -> trx.repoUsers().create("charlie", new PasswordValidationInfo("hash")));
        Either<ChannelError, String> result = channelService.joinPrivateChannel(charlie.id(), "token123");

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<ChannelError, String>) result).value()).isInstanceOf(ChannelError.InvitationAlreadyUsed.class);
    }
}