package pt.isel.services.channels;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.channels.ChannelMember;
import pt.isel.domain.common.ChannelError;
import pt.isel.domain.common.Either;
import pt.isel.domain.common.EitherAssert;
import pt.isel.domain.common.MessageError;
import pt.isel.domain.invitations.Invitation;
import pt.isel.domain.users.UserInfo;
import pt.isel.services.AbstractServiceTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelServiceTest extends AbstractServiceTest {

    private ChannelService channelService;

    @BeforeEach
    void setUp() {
        super.setUpBaseState();
        channelService = new ChannelService(trxManager, clock);
    }

    @Test
    void CreateChannel_ValidInput_ReturnsSuccess() {
        Either<ChannelError, Channel> result = channelService.createChannel("General", alice.id(), true);

        Channel channel = EitherAssert.assertRight(result);
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
    void CreateChannel_ChannelAlreadyExists_ReturnsLeft() {
        channelService.createChannel("General", alice.id(), true);
        Either<ChannelError, Channel> result = channelService.createChannel("General", bob.id(), true);

        EitherAssert.assertLeft(result, ChannelError.ChannelAlreadyExists.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void CreateChannel_EmptyName_ReturnsLeft(String invalidName) {
        EitherAssert.assertLeft(channelService.createChannel(invalidName, alice.id(), true));
    }

    @Test
    void CreateChannel_NameTooLong_ReturnsLeft() {
        String longName = "a".repeat(31);
        Either<ChannelError, Channel> result = channelService.createChannel(longName, alice.id(), true);

        EitherAssert.assertLeft(result, ChannelError.InvalidChannelNameLength.class);
    }

    @Test
    void CreateChannel_UserNotFound_ReturnsLeft() {
        Either<ChannelError, Channel> result = channelService.createChannel("General", 999L, true);

        EitherAssert.assertLeft(result, ChannelError.UserNotFound.class);
    }

    @Test
    void GetChannelById_ValidId_ReturnsSuccess() {
        Channel created = EitherAssert.assertRight(channelService.createChannel("General", alice.id(), true));
        Either<ChannelError, Channel> result = channelService.getChannelById(created.id());

        assertThat(EitherAssert.assertRight(result).id()).isEqualTo(created.id());
    }

    @Test
    void GetChannelById_InvalidId_ReturnsLeft() {
        Either<ChannelError, Channel> result = channelService.getChannelById(999L);

        EitherAssert.assertLeft(result, ChannelError.ChannelNotFound.class);
    }

    @Test
    void DeleteChannel_ValidInput_ReturnsSuccess() {
        Channel created = EitherAssert.assertRight(channelService.createChannel("General", alice.id(), true));
        Either<ChannelError, String> result = channelService.deleteChannel(alice.id(), created.id());

        EitherAssert.assertRight(result);
        EitherAssert.assertLeft(channelService.getChannelById(created.id()));
    }

    @Test
    void DeleteChannel_NotOwner_ReturnsLeft() {
        Channel created = EitherAssert.assertRight(channelService.createChannel("General", alice.id(), true));
        Either<ChannelError, String> result = channelService.deleteChannel(bob.id(), created.id());

        EitherAssert.assertLeft(result, ChannelError.UserNotOwner.class);
    }

    @Test
    void GetJoinedChannels_ValidInput_ReturnsSuccess() {
        channelService.createChannel("C1", alice.id(), true);
        channelService.createChannel("C2", alice.id(), true);

        Either<ChannelError, List<Channel>> result = channelService.getJoinedChannels(alice.id(), 10, 0);

        assertThat(EitherAssert.assertRight(result)).hasSize(2);
    }

    @Test
    void EditChannel_ValidInput_ReturnsSuccess() {
        Channel created = EitherAssert.assertRight(channelService.createChannel("General", alice.id(), true));
        Either<ChannelError, Channel> result = channelService.editChannel(alice.id(), created.id(), "NewName", false);

        Channel updated = EitherAssert.assertRight(result);
        assertThat(updated.name()).isEqualTo("NewName");
        assertThat(updated.isPublic()).isFalse();
    }

    @Test
    void EditChannel_ChannelAlreadyExists_ReturnsLeft() {
        Channel c1 = EitherAssert.assertRight(channelService.createChannel("General", alice.id(), true));
        channelService.createChannel("Random", alice.id(), true);

        Either<ChannelError, Channel> result = channelService.editChannel(alice.id(), c1.id(), "Random", true);

        EitherAssert.assertLeft(result, ChannelError.ChannelAlreadyExists.class);
    }

    @Test
    void EditChannel_UserNotFound_ReturnsLeft() {
        Channel c1 = EitherAssert.assertRight(channelService.createChannel("General", alice.id(), true));
        Either<ChannelError, Channel> result = channelService.editChannel(999L, c1.id(), "NewName", true);

        EitherAssert.assertLeft(result, ChannelError.UserNotFound.class);
    }

    @Test
    void EditChannel_ChannelNotFound_ReturnsLeft() {
        Either<ChannelError, Channel> result = channelService.editChannel(alice.id(), 999L, "NewName", true);

        EitherAssert.assertLeft(result, ChannelError.ChannelNotFound.class);
    }

    @Test
    void GetAccessType_ValidInput_ReturnsSuccess() {
        Channel created = EitherAssert.assertRight(channelService.createChannel("General", alice.id(), true));
        channelService.joinPublicChannel(bob.id(), created.id());

        Either<MessageError, AccessType> result = channelService.getAccessType(alice.id(), bob.id(), created.id());

        assertThat(EitherAssert.assertRight(result)).isEqualTo(AccessType.READ_WRITE);
    }

    @Test
    void EditMemberAccess_ValidInput_ReturnsSuccess() {
        Channel created = EitherAssert.assertRight(channelService.createChannel("General", alice.id(), true));
        channelService.joinPublicChannel(bob.id(), created.id());

        Either<ChannelError, ChannelMember> result = channelService.editMemberAccess(alice.id(), created.id(), bob.id(), AccessType.READ_ONLY);

        assertThat(EitherAssert.assertRight(result).accessType()).isEqualTo(AccessType.READ_ONLY);
    }

    @Test
    void EditMemberAccess_TargetIsOwner_ReturnsLeft() {
        Channel created = EitherAssert.assertRight(channelService.createChannel("General", alice.id(), true));
        Either<ChannelError, ChannelMember> result = channelService.editMemberAccess(alice.id(), created.id(), alice.id(), AccessType.READ_ONLY);

        EitherAssert.assertLeft(result, ChannelError.UserIsOwner.class);
    }

    @Test
    void EditMemberAccess_TargetNotInChannel_ReturnsLeft() {
        Channel c1 = EitherAssert.assertRight(channelService.createChannel("General", alice.id(), true));

        Either<ChannelError, ChannelMember> result = channelService.editMemberAccess(alice.id(), c1.id(), bob.id(), AccessType.READ_WRITE);

        EitherAssert.assertLeft(result, ChannelError.UserNotInChannel.class);
    }

    @Test
    void SearchChannels_ValidInput_ReturnsSuccess() {
        channelService.createChannel("Java Devs", alice.id(), true);
        channelService.createChannel("Secret", alice.id(), false);

        Either<ChannelError, List<Channel>> result = channelService.searchChannels("java", 10, 0);

        List<Channel> channels = EitherAssert.assertRight(result);
        assertThat(channels).hasSize(1);
        assertThat(channels.getFirst().name()).isEqualTo("Java Devs");
    }

    @Test
    void JoinPublicChannel_ValidInput_ReturnsSuccess() {
        Channel created = EitherAssert.assertRight(channelService.createChannel("General", alice.id(), true));
        Either<ChannelError, String> result = channelService.joinPublicChannel(bob.id(), created.id());

        EitherAssert.assertRight(result);
    }

    @Test
    void JoinPublicChannel_PrivateChannel_ReturnsLeft() {
        Channel created = EitherAssert.assertRight(channelService.createChannel("Secret", alice.id(), false));
        Either<ChannelError, String> result = channelService.joinPublicChannel(bob.id(), created.id());

        EitherAssert.assertLeft(result, ChannelError.ChannelIsPrivate.class);
    }

    @Test
    void JoinPrivateChannel_ValidInput_ReturnsSuccess() {
        Channel created = EitherAssert.assertRight(channelService.createChannel("Secret", alice.id(), false));

        Invitation inv = trxManager.run(trx -> insertInvitation(trx, "token123", alice, created, AccessType.READ_ONLY, LocalDateTime.now(clock).plusDays(1)));

        Either<ChannelError, String> result = channelService.joinPrivateChannel(bob.id(), inv.token());
        EitherAssert.assertRight(result);

        Either<MessageError, AccessType> access = channelService.getAccessType(bob.id(), bob.id(), created.id());
        assertThat(EitherAssert.assertRight(access)).isEqualTo(AccessType.READ_ONLY);
    }

    @Test
    void LeaveChannel_ValidInput_ReturnsSuccess() {
        Channel created = EitherAssert.assertRight(channelService.createChannel("General", alice.id(), true));
        channelService.joinPublicChannel(bob.id(), created.id());

        Either<ChannelError, String> result = channelService.leaveChannel(created.id(), bob.id());
        EitherAssert.assertRight(result);
    }

    @Test
    void LeaveChannel_OwnerCannotLeave_ReturnsLeft() {
        Channel created = EitherAssert.assertRight(channelService.createChannel("General", alice.id(), true));
        Either<ChannelError, String> result = channelService.leaveChannel(created.id(), alice.id());

        EitherAssert.assertLeft(result, ChannelError.OwnerCannotLeave.class);
    }

    @Test
    void DeleteChannel_UserNotFound_ReturnsLeft() {
        Either<ChannelError, String> result = channelService.deleteChannel(999L, 1L);

        EitherAssert.assertLeft(result, ChannelError.UserNotFound.class);
    }

    @Test
    void DeleteChannel_ChannelNotFound_ReturnsLeft() {
        Either<ChannelError, String> result = channelService.deleteChannel(alice.id(), 999L);

        EitherAssert.assertLeft(result, ChannelError.ChannelNotFound.class);
    }

    @Test
    void GetJoinedChannels_UserNotFound_ReturnsLeft() {
        Either<ChannelError, List<Channel>> result = channelService.getJoinedChannels(999L, 10, 0);

        EitherAssert.assertLeft(result, ChannelError.UserNotFound.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void EditChannel_EmptyName_ReturnsLeft(String invalidName) {
        Channel created = EitherAssert.assertRight(channelService.createChannel("General", alice.id(), true));
        EitherAssert.assertLeft(channelService.editChannel(alice.id(), created.id(), invalidName, true));
    }

    @Test
    void EditChannel_NotOwner_ReturnsLeft() {
        Channel created = EitherAssert.assertRight(channelService.createChannel("General", alice.id(), true));
        Either<ChannelError, Channel> result = channelService.editChannel(bob.id(), created.id(), "NewName", true);

        EitherAssert.assertLeft(result, ChannelError.UserNotOwner.class);
    }

    @Test
    void GetAccessType_TargetNotInChannel_ReturnsLeft() {
        Channel created = EitherAssert.assertRight(channelService.createChannel("General", alice.id(), true));
        Either<MessageError, AccessType> result = channelService.getAccessType(alice.id(), bob.id(), created.id());

        EitherAssert.assertLeft(result, MessageError.UserNotInChannel.class);
    }

    @Test
    void GetAccessType_RequesterNotAuthorized_ReturnsLeft() {
        Channel created = EitherAssert.assertRight(channelService.createChannel("General", alice.id(), true));
        channelService.joinPublicChannel(bob.id(), created.id());

        Either<MessageError, AccessType> result = channelService.getAccessType(charlie.id(), bob.id(), created.id());

        EitherAssert.assertLeft(result, MessageError.UserNotAuthorized.class);
    }

    @Test
    void EditMemberAccess_NotOwner_ReturnsLeft() {
        Channel created = EitherAssert.assertRight(channelService.createChannel("General", alice.id(), true));
        channelService.joinPublicChannel(bob.id(), created.id());

        Either<ChannelError, ChannelMember> result = channelService.editMemberAccess(bob.id(), created.id(), bob.id(), AccessType.READ_ONLY);

        EitherAssert.assertLeft(result, ChannelError.UserNotOwner.class);
    }

    @Test
    void SearchChannels_EmptyQuery_ReturnsSuccess() {
        channelService.createChannel("Java Devs", alice.id(), true);
        channelService.createChannel("Secret", alice.id(), false);

        Either<ChannelError, List<Channel>> result = channelService.searchChannels("", 10, 0);

        assertThat(EitherAssert.assertRight(result)).hasSize(1);
    }

    @Test
    void JoinPublicChannel_AlreadyInChannel_ReturnsLeft() {
        Channel created = EitherAssert.assertRight(channelService.createChannel("General", alice.id(), true));
        channelService.joinPublicChannel(bob.id(), created.id());

        Either<ChannelError, String> result = channelService.joinPublicChannel(bob.id(), created.id());

        EitherAssert.assertLeft(result, ChannelError.UserAlreadyInChannel.class);
    }

    @Test
    void JoinPrivateChannel_TokenNotFound_ReturnsLeft() {
        Either<ChannelError, String> result = channelService.joinPrivateChannel(bob.id(), "invalid-token");

        EitherAssert.assertLeft(result, ChannelError.TokenNotFound.class);
    }

    @Test
    void JoinPrivateChannel_ExpiredToken_ReturnsLeft() {
        Channel created = EitherAssert.assertRight(channelService.createChannel("Secret", alice.id(), false));
        trxManager.run(trx -> insertInvitation(trx, "expired-token", alice, created, AccessType.READ_ONLY, LocalDateTime.now(clock).minusDays(1)));

        Either<ChannelError, String> result = channelService.joinPrivateChannel(bob.id(), "expired-token");

        EitherAssert.assertLeft(result, ChannelError.InvitationExpired.class);
    }

    @Test
    void LeaveChannel_ChannelNotFound_ReturnsLeft() {
        Either<ChannelError, String> result = channelService.leaveChannel(999L, bob.id());

        EitherAssert.assertLeft(result, ChannelError.ChannelNotFound.class);
    }

    @Test
    void LeaveChannel_UserNotInChannel_ReturnsLeft() {
        Channel created = EitherAssert.assertRight(channelService.createChannel("General", alice.id(), true));
        Either<ChannelError, String> result = channelService.leaveChannel(created.id(), bob.id());

        EitherAssert.assertLeft(result, ChannelError.UserNotInChannel.class);
    }

    @Test
    void GetUsersInChannel_ValidInput_ReturnsSuccess() {
        Channel created = EitherAssert.assertRight(channelService.createChannel("General", alice.id(), true));
        channelService.joinPublicChannel(bob.id(), created.id());

        Either<ChannelError, List<UserInfo>> result = channelService.getUsersInChannel(created.id(), 10, 0);

        assertThat(EitherAssert.assertRight(result)).hasSize(2);
    }

    @Test
    void EditChannel_NameTooLong_ReturnsLeft() {
        Channel created = EitherAssert.assertRight(channelService.createChannel("General", alice.id(), true));
        String longName = "a".repeat(31);

        Either<ChannelError, Channel> result = channelService.editChannel(alice.id(), created.id(), longName, true);

        EitherAssert.assertLeft(result, ChannelError.InvalidChannelNameLength.class);
    }

    @Test
    void EditMemberAccess_UserNotFound_ReturnsLeft() {
        Channel created = EitherAssert.assertRight(channelService.createChannel("General", alice.id(), true));

        Either<ChannelError, ChannelMember> result = channelService.editMemberAccess(alice.id(), created.id(), 999L, AccessType.READ_ONLY);

        EitherAssert.assertLeft(result, ChannelError.UserNotFound.class);
    }

    @Test
    void JoinPublicChannel_UserNotFound_ReturnsLeft() {
        Channel created = EitherAssert.assertRight(channelService.createChannel("General", alice.id(), true));

        Either<ChannelError, String> result = channelService.joinPublicChannel(999L, created.id());

        EitherAssert.assertLeft(result, ChannelError.UserNotFound.class);
    }

    @Test
    void JoinPublicChannel_ChannelNotFound_ReturnsLeft() {
        Either<ChannelError, String> result = channelService.joinPublicChannel(bob.id(), 999L);

        EitherAssert.assertLeft(result, ChannelError.ChannelNotFound.class);
    }

    @Test
    void JoinPrivateChannel_UserNotFound_ReturnsLeft() {
        Either<ChannelError, String> result = channelService.joinPrivateChannel(999L, "some-token");

        EitherAssert.assertLeft(result, ChannelError.UserNotFound.class);
    }

    @Test
    void JoinPrivateChannel_AlreadyInChannel_ReturnsLeft() {
        Channel created = EitherAssert.assertRight(channelService.createChannel("Secret", alice.id(), false));
        trxManager.run(trx -> insertMember(trx, bob, created, AccessType.READ_ONLY));
        trxManager.run(trx -> insertInvitation(trx, "token123", alice, created, AccessType.READ_ONLY, LocalDateTime.now(clock).plusDays(1)));

        Either<ChannelError, String> result = channelService.joinPrivateChannel(bob.id(), "token123");

        EitherAssert.assertLeft(result, ChannelError.UserAlreadyInChannel.class);
    }

    @Test
    void JoinPrivateChannel_InvitationAlreadyUsed_ReturnsLeft() {
        Channel created = EitherAssert.assertRight(channelService.createChannel("Secret", alice.id(), false));
        trxManager.run(trx -> insertInvitation(trx, "token123", alice, created, AccessType.READ_ONLY, LocalDateTime.now(clock).plusDays(1)));

        channelService.joinPrivateChannel(bob.id(), "token123");

        Either<ChannelError, String> result = channelService.joinPrivateChannel(charlie.id(), "token123");

        EitherAssert.assertLeft(result, ChannelError.InvitationAlreadyUsed.class);
    }
}