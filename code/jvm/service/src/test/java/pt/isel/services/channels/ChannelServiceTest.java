package pt.isel.services.channels;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        assertInstanceOf(Either.Right.class, result);
        Channel channel = ((Either.Right<ChannelError, Channel>) result).value();
        assertEquals("General", channel.name());
        assertEquals(alice.id(), channel.owner().id());
        assertTrue(channel.isPublic());

        trxManager.run(trx -> {
            ChannelMember member = trx.repoMemberships().findUserInChannel(alice.id(), channel.id());
            assertNotNull(member);
            assertEquals(AccessType.READ_WRITE, member.accessType());
            return null;
        });
    }

    @Test
    void testCreateChannel_EmptyName() {
        assertInstanceOf(Either.Left.class, channelService.createChannel("", alice.id(), true));
        assertInstanceOf(Either.Left.class, channelService.createChannel(null, alice.id(), true));
    }

    @Test
    void testCreateChannel_NameTooLong() {
        String longName = "a".repeat(31);
        Either<ChannelError, Channel> result = channelService.createChannel(longName, alice.id(), true);
        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(ChannelError.InvalidChannelNameLength.class, ((Either.Left<ChannelError, Channel>) result).value());
    }

    @Test
    void testCreateChannel_UserNotFound() {
        Either<ChannelError, Channel> result = channelService.createChannel("General", 999L, true);
        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(ChannelError.UserNotFound.class, ((Either.Left<ChannelError, Channel>) result).value());
    }

    @Test
    void testGetChannelById_Success() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        Either<ChannelError, Channel> result = channelService.getChannelById(created.id());

        assertInstanceOf(Either.Right.class, result);
        assertEquals(created.id(), ((Either.Right<ChannelError, Channel>) result).value().id());
    }

    @Test
    void testGetChannelById_NotFound() {
        Either<ChannelError, Channel> result = channelService.getChannelById(999L);
        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(ChannelError.ChannelNotFound.class, ((Either.Left<ChannelError, Channel>) result).value());
    }

    @Test
    void testDeleteChannel_Success() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        Either<ChannelError, String> result = channelService.deleteChannel(alice.id(), created.id());

        assertInstanceOf(Either.Right.class, result);
        assertInstanceOf(Either.Left.class, channelService.getChannelById(created.id()));
    }

    @Test
    void testDeleteChannel_NotOwner() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        Either<ChannelError, String> result = channelService.deleteChannel(bob.id(), created.id());

        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(ChannelError.UserNotOwner.class, ((Either.Left<ChannelError, String>) result).value());
    }

    @Test
    void testGetJoinedChannels_Success() {
        channelService.createChannel("C1", alice.id(), true);
        channelService.createChannel("C2", alice.id(), true);

        Either<ChannelError, List<Channel>> result = channelService.getJoinedChannels(alice.id(), 10, 0);
        assertInstanceOf(Either.Right.class, result);
        assertEquals(2, ((Either.Right<ChannelError, List<Channel>>) result).value().size());
    }

    @Test
    void testEditChannel_Success() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        Either<ChannelError, Channel> result = channelService.editChannel(alice.id(), created.id(), "NewName", false);

        assertInstanceOf(Either.Right.class, result);
        Channel updated = ((Either.Right<ChannelError, Channel>) result).value();
        assertEquals("NewName", updated.name());
        assertFalse(updated.isPublic());
    }

    @Test
    void testGetAccessType_Success() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        channelService.joinPublicChannel(bob.id(), created.id());

        Either<MessageError, AccessType> result = channelService.getAccessType(alice.id(), bob.id(), created.id());
        assertInstanceOf(Either.Right.class, result);
        assertEquals(AccessType.READ_WRITE, ((Either.Right<MessageError, AccessType>) result).value());
    }

    @Test
    void testEditMemberAccess_Success() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        channelService.joinPublicChannel(bob.id(), created.id());

        Either<ChannelError, ChannelMember> result = channelService.editMemberAccess(alice.id(), created.id(), bob.id(), AccessType.READ_ONLY);
        assertInstanceOf(Either.Right.class, result);
        assertEquals(AccessType.READ_ONLY, ((Either.Right<ChannelError, ChannelMember>) result).value().accessType());
    }

    @Test
    void testEditMemberAccess_TargetIsOwner() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        Either<ChannelError, ChannelMember> result = channelService.editMemberAccess(alice.id(), created.id(), alice.id(), AccessType.READ_ONLY);

        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(ChannelError.UserIsOwner.class, ((Either.Left<ChannelError, ChannelMember>) result).value());
    }

    @Test
    void testSearchChannels_Success() {
        channelService.createChannel("Java Devs", alice.id(), true);
        channelService.createChannel("Secret", alice.id(), false);

        Either<ChannelError, List<Channel>> result = channelService.searchChannels("java", 10, 0);
        assertInstanceOf(Either.Right.class, result);
        List<Channel> channels = ((Either.Right<ChannelError, List<Channel>>) result).value();
        assertEquals(1, channels.size());
        assertEquals("Java Devs", channels.getFirst().name());
    }

    @Test
    void testJoinPublicChannel_Success() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        Either<ChannelError, String> result = channelService.joinPublicChannel(bob.id(), created.id());

        assertInstanceOf(Either.Right.class, result);
    }

    @Test
    void testJoinPublicChannel_PrivateChannel() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("Secret", alice.id(), false)).value();
        Either<ChannelError, String> result = channelService.joinPublicChannel(bob.id(), created.id());

        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(ChannelError.ChannelIsPrivate.class, ((Either.Left<ChannelError, String>) result).value());
    }

    @Test
    void testJoinPrivateChannel_Success() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("Secret", alice.id(), false)).value();

        Invitation inv = trxManager.run(trx -> trx.repoInvitations().create(
                "token123", new UserInfo(alice.id(), alice.username()), created, AccessType.READ_ONLY, LocalDateTime.now(ZoneOffset.UTC).plusDays(1)
        ));

        Either<ChannelError, String> result = channelService.joinPrivateChannel(bob.id(), inv.token());
        assertInstanceOf(Either.Right.class, result);

        Either<MessageError, AccessType> access = channelService.getAccessType(bob.id(), bob.id(), created.id());
        assertEquals(AccessType.READ_ONLY, ((Either.Right<MessageError, AccessType>) access).value());
    }

    @Test
    void testLeaveChannel_Success() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        channelService.joinPublicChannel(bob.id(), created.id());

        Either<ChannelError, String> result = channelService.leaveChannel(created.id(), bob.id());
        assertInstanceOf(Either.Right.class, result);
    }

    @Test
    void testLeaveChannel_OwnerCannotLeave() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        Either<ChannelError, String> result = channelService.leaveChannel(created.id(), alice.id());

        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(ChannelError.OwnerCannotLeave.class, ((Either.Left<ChannelError, String>) result).value());
    }

    @Test
    void testDeleteChannel_UserNotFound() {
        Either<ChannelError, String> result = channelService.deleteChannel(999L, 1L);
        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(ChannelError.UserNotFound.class, ((Either.Left<ChannelError, String>) result).value());
    }

    @Test
    void testDeleteChannel_ChannelNotFound() {
        Either<ChannelError, String> result = channelService.deleteChannel(alice.id(), 999L);
        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(ChannelError.ChannelNotFound.class, ((Either.Left<ChannelError, String>) result).value());
    }

    @Test
    void testGetJoinedChannels_UserNotFound() {
        Either<ChannelError, List<Channel>> result = channelService.getJoinedChannels(999L, 10, 0);
        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(ChannelError.UserNotFound.class, ((Either.Left<ChannelError, List<Channel>>) result).value());
    }

    @Test
    void testEditChannel_EmptyName() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        assertInstanceOf(Either.Left.class, channelService.editChannel(alice.id(), created.id(), "", true));
    }

    @Test
    void testEditChannel_NotOwner() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        Either<ChannelError, Channel> result = channelService.editChannel(bob.id(), created.id(), "NewName", true);
        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(ChannelError.UserNotOwner.class, ((Either.Left<ChannelError, Channel>) result).value());
    }

    @Test
    void testGetAccessType_TargetNotInChannel() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        Either<MessageError, AccessType> result = channelService.getAccessType(alice.id(), bob.id(), created.id());
        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(MessageError.UserNotInChannel.class, ((Either.Left<MessageError, AccessType>) result).value());
    }

    @Test
    void testGetAccessType_RequesterNotAuthorized() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        channelService.joinPublicChannel(bob.id(), created.id());

        User charlie = trxManager.run(trx -> trx.repoUsers().create("charlie", new PasswordValidationInfo("hash")));

        Either<MessageError, AccessType> result = channelService.getAccessType(charlie.id(), bob.id(), created.id());
        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(MessageError.UserNotAuthorized.class, ((Either.Left<MessageError, AccessType>) result).value());
    }

    @Test
    void testEditMemberAccess_NotOwner() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        channelService.joinPublicChannel(bob.id(), created.id());

        Either<ChannelError, ChannelMember> result = channelService.editMemberAccess(bob.id(), created.id(), bob.id(), AccessType.READ_ONLY);
        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(ChannelError.UserNotOwner.class, ((Either.Left<ChannelError, ChannelMember>) result).value());
    }

    @Test
    void testSearchChannels_EmptyQuery() {
        channelService.createChannel("Java Devs", alice.id(), true);
        channelService.createChannel("Secret", alice.id(), false);

        Either<ChannelError, List<Channel>> result = channelService.searchChannels("", 10, 0);
        assertInstanceOf(Either.Right.class, result);
        assertEquals(1, ((Either.Right<ChannelError, List<Channel>>) result).value().size());
    }

    @Test
    void testJoinPublicChannel_AlreadyInChannel() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        channelService.joinPublicChannel(bob.id(), created.id());

        Either<ChannelError, String> result = channelService.joinPublicChannel(bob.id(), created.id());
        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(ChannelError.UserAlreadyInChannel.class, ((Either.Left<ChannelError, String>) result).value());
    }

    @Test
    void testJoinPrivateChannel_TokenNotFound() {
        Either<ChannelError, String> result = channelService.joinPrivateChannel(bob.id(), "invalid-token");
        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(ChannelError.TokenNotFound.class, ((Either.Left<ChannelError, String>) result).value());
    }

    @Test
    void testJoinPrivateChannel_ExpiredToken() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("Secret", alice.id(), false)).value();
        trxManager.run(trx -> trx.repoInvitations().create(
                "expired-token", new UserInfo(alice.id(), alice.username()), created, AccessType.READ_ONLY, LocalDateTime.now(ZoneOffset.UTC).minusDays(1)
        ));

        Either<ChannelError, String> result = channelService.joinPrivateChannel(bob.id(), "expired-token");
        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(ChannelError.InvitationExpired.class, ((Either.Left<ChannelError, String>) result).value());
    }

    @Test
    void testLeaveChannel_ChannelNotFound() {
        Either<ChannelError, String> result = channelService.leaveChannel(999L, bob.id());
        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(ChannelError.ChannelNotFound.class, ((Either.Left<ChannelError, String>) result).value());
    }

    @Test
    void testLeaveChannel_UserNotInChannel() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        Either<ChannelError, String> result = channelService.leaveChannel(created.id(), bob.id());
        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(ChannelError.UserNotInChannel.class, ((Either.Left<ChannelError, String>) result).value());
    }

    @Test
    void testGetUsersInChannel_Success() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        channelService.joinPublicChannel(bob.id(), created.id());

        Either<ChannelError, List<UserInfo>> result = channelService.getUsersInChannel(created.id(), 10, 0);

        assertInstanceOf(Either.Right.class, result);
        assertEquals(2, ((Either.Right<ChannelError, List<UserInfo>>) result).value().size());
    }

    @Test
    void testEditChannel_NameTooLong() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();
        String longName = "a".repeat(31);

        Either<ChannelError, Channel> result = channelService.editChannel(alice.id(), created.id(), longName, true);

        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(ChannelError.InvalidChannelNameLength.class, ((Either.Left<ChannelError, Channel>) result).value());
    }

    @Test
    void testEditMemberAccess_UserNotFound() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();

        Either<ChannelError, ChannelMember> result = channelService.editMemberAccess(alice.id(), created.id(), 999L, AccessType.READ_ONLY);

        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(ChannelError.UserNotFound.class, ((Either.Left<ChannelError, ChannelMember>) result).value());
    }

    @Test
    void testJoinPublicChannel_UserNotFound() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("General", alice.id(), true)).value();

        Either<ChannelError, String> result = channelService.joinPublicChannel(999L, created.id());

        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(ChannelError.UserNotFound.class, ((Either.Left<ChannelError, String>) result).value());
    }

    @Test
    void testJoinPublicChannel_ChannelNotFound() {
        Either<ChannelError, String> result = channelService.joinPublicChannel(bob.id(), 999L);

        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(ChannelError.ChannelNotFound.class, ((Either.Left<ChannelError, String>) result).value());
    }

    @Test
    void testJoinPrivateChannel_UserNotFound() {
        Either<ChannelError, String> result = channelService.joinPrivateChannel(999L, "some-token");

        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(ChannelError.UserNotFound.class, ((Either.Left<ChannelError, String>) result).value());
    }

    @Test
    void testJoinPrivateChannel_AlreadyInChannel() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("Secret", alice.id(), false)).value();
        trxManager.run(trx -> trx.repoMemberships().addUserToChannel(new UserInfo(bob.id(), bob.username()), created, AccessType.READ_ONLY));
        trxManager.run(trx -> trx.repoInvitations().create(
                "token123", new UserInfo(alice.id(), alice.username()), created, AccessType.READ_ONLY, LocalDateTime.now(ZoneOffset.UTC).plusDays(1)
        ));

        Either<ChannelError, String> result = channelService.joinPrivateChannel(bob.id(), "token123");

        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(ChannelError.UserAlreadyInChannel.class, ((Either.Left<ChannelError, String>) result).value());
    }

    @Test
    void testJoinPrivateChannel_InvitationAlreadyUsed() {
        Channel created = ((Either.Right<ChannelError, Channel>) channelService.createChannel("Secret", alice.id(), false)).value();
        trxManager.run(trx -> trx.repoInvitations().create(
                "token123", new UserInfo(alice.id(), alice.username()), created, AccessType.READ_ONLY, LocalDateTime.now(ZoneOffset.UTC).plusDays(1)
        ));

        channelService.joinPrivateChannel(bob.id(), "token123");

        User charlie = trxManager.run(trx -> trx.repoUsers().create("charlie", new PasswordValidationInfo("hash")));
        Either<ChannelError, String> result = channelService.joinPrivateChannel(charlie.id(), "token123");

        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(ChannelError.InvitationAlreadyUsed.class, ((Either.Left<ChannelError, String>) result).value());
    }
}