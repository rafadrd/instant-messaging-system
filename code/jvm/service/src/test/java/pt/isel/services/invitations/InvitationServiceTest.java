package pt.isel.services.invitations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.common.Either;
import pt.isel.domain.common.InvitationError;
import pt.isel.domain.invitations.Invitation;
import pt.isel.domain.invitations.InvitationStatus;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.User;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.mem.TransactionManagerInMem;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InvitationServiceTest {

    private TransactionManagerInMem trxManager;
    private InvitationService invitationService;

    private User alice;
    private User bob;
    private User charlie;
    private Channel channel;

    @BeforeEach
    void setUp() {
        trxManager = new TransactionManagerInMem();
        invitationService = new InvitationService(trxManager);

        alice = trxManager.run(trx -> trx.repoUsers().create("alice", new PasswordValidationInfo("hash")));
        bob = trxManager.run(trx -> trx.repoUsers().create("bob", new PasswordValidationInfo("hash")));
        charlie = trxManager.run(trx -> trx.repoUsers().create("charlie", new PasswordValidationInfo("hash")));

        channel = trxManager.run(trx -> {
            UserInfo aliceInfo = new UserInfo(alice.id(), alice.username());
            Channel c = trx.repoChannels().create("Secret", aliceInfo, false);
            trx.repoMemberships().addUserToChannel(aliceInfo, c, AccessType.READ_WRITE);
            trx.repoMemberships().addUserToChannel(new UserInfo(bob.id(), bob.username()), c, AccessType.READ_WRITE);
            trx.repoMemberships().addUserToChannel(new UserInfo(charlie.id(), charlie.username()), c, AccessType.READ_ONLY);
            return c;
        });
    }

    @Test
    void testCreateInvitation_SuccessByOwner() {
        LocalDateTime expiry = LocalDateTime.now().plusDays(1);
        Either<InvitationError, Invitation> result = invitationService.createInvitation(alice.id(), channel.id(), AccessType.READ_ONLY, expiry);

        assertThat(result).isInstanceOf(Either.Right.class);
        Invitation inv = ((Either.Right<InvitationError, Invitation>) result).value();
        assertThat(inv.token()).isNotNull();
        assertThat(inv.accessType()).isEqualTo(AccessType.READ_ONLY);
    }

    @Test
    void testCreateInvitation_SuccessByReadWriteMember() {
        LocalDateTime expiry = LocalDateTime.now().plusDays(1);
        Either<InvitationError, Invitation> result = invitationService.createInvitation(bob.id(), channel.id(), AccessType.READ_WRITE, expiry);

        assertThat(result).isInstanceOf(Either.Right.class);
    }

    @Test
    void testCreateInvitation_FailureByReadOnlyMember() {
        LocalDateTime expiry = LocalDateTime.now().plusDays(1);
        Either<InvitationError, Invitation> result = invitationService.createInvitation(charlie.id(), channel.id(), AccessType.READ_ONLY, expiry);

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<InvitationError, Invitation>) result).value()).isInstanceOf(InvitationError.UserNotAuthorized.class);
    }

    @Test
    void testCreateInvitation_InvalidExpiration() {
        LocalDateTime expiry = LocalDateTime.now().minusDays(1);
        Either<InvitationError, Invitation> result = invitationService.createInvitation(alice.id(), channel.id(), AccessType.READ_ONLY, expiry);

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<InvitationError, Invitation>) result).value()).isInstanceOf(InvitationError.InvalidExpirationTime.class);
    }

    @Test
    void testGetInvitationsForChannel_Success() {
        invitationService.createInvitation(alice.id(), channel.id(), AccessType.READ_ONLY, LocalDateTime.now().plusDays(1));
        invitationService.createInvitation(bob.id(), channel.id(), AccessType.READ_WRITE, LocalDateTime.now().plusDays(1));

        Either<InvitationError, List<Invitation>> result = invitationService.getInvitationsForChannel(alice.id(), channel.id());

        assertThat(result).isInstanceOf(Either.Right.class);
        assertThat(((Either.Right<InvitationError, List<Invitation>>) result).value()).hasSize(2);
    }

    @Test
    void testGetInvitationsForChannel_UserNotFound() {
        Either<InvitationError, List<Invitation>> result = invitationService.getInvitationsForChannel(999L, channel.id());

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<InvitationError, List<Invitation>>) result).value()).isInstanceOf(InvitationError.UserNotFound.class);
    }

    @Test
    void testGetInvitationsForChannel_ChannelNotFound() {
        Either<InvitationError, List<Invitation>> result = invitationService.getInvitationsForChannel(alice.id(), 999L);

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<InvitationError, List<Invitation>>) result).value()).isInstanceOf(InvitationError.ChannelNotFound.class);
    }

    @Test
    void testGetInvitationsForChannel_UserNotInChannel() {
        User dave = trxManager.run(trx -> trx.repoUsers().create("dave", new PasswordValidationInfo("hash")));
        Either<InvitationError, List<Invitation>> result = invitationService.getInvitationsForChannel(dave.id(), channel.id());

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<InvitationError, List<Invitation>>) result).value()).isInstanceOf(InvitationError.UserNotInChannel.class);
    }

    @Test
    void testRevokeInvitation_SuccessByOwner() {
        Invitation inv = ((Either.Right<InvitationError, Invitation>) invitationService.createInvitation(
                alice.id(), channel.id(), AccessType.READ_ONLY, LocalDateTime.now().plusDays(1)
        )).value();

        Either<InvitationError, String> result = invitationService.revokeInvitation(alice.id(), channel.id(), inv.id());
        assertThat(result).isInstanceOf(Either.Right.class);

        trxManager.run(trx -> {
            assertThat(trx.repoInvitations().findById(inv.id()).status()).isEqualTo(InvitationStatus.REJECTED);
            return null;
        });
    }

    @Test
    void testRevokeInvitation_FailureNotOwner() {
        Invitation inv = ((Either.Right<InvitationError, Invitation>) invitationService.createInvitation(
                alice.id(), channel.id(), AccessType.READ_ONLY, LocalDateTime.now().plusDays(1)
        )).value();

        Either<InvitationError, String> result = invitationService.revokeInvitation(bob.id(), channel.id(), inv.id());

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<InvitationError, String>) result).value()).isInstanceOf(InvitationError.UserNotAuthorized.class);
    }

    @Test
    void testCreateInvitation_UserNotFound() {
        Either<InvitationError, Invitation> result = invitationService.createInvitation(999L, channel.id(), AccessType.READ_ONLY, LocalDateTime.now().plusDays(1));

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<InvitationError, Invitation>) result).value()).isInstanceOf(InvitationError.UserNotFound.class);
    }

    @Test
    void testCreateInvitation_ChannelNotFound() {
        Either<InvitationError, Invitation> result = invitationService.createInvitation(alice.id(), 999L, AccessType.READ_ONLY, LocalDateTime.now().plusDays(1));

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<InvitationError, Invitation>) result).value()).isInstanceOf(InvitationError.ChannelNotFound.class);
    }

    @Test
    void testCreateInvitation_UserNotInChannel() {
        User dave = trxManager.run(trx -> trx.repoUsers().create("dave", new PasswordValidationInfo("hash")));
        Either<InvitationError, Invitation> result = invitationService.createInvitation(dave.id(), channel.id(), AccessType.READ_ONLY, LocalDateTime.now().plusDays(1));

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<InvitationError, Invitation>) result).value()).isInstanceOf(InvitationError.UserNotInChannel.class);
    }

    @Test
    void testRevokeInvitation_InvitationNotFound() {
        Either<InvitationError, String> result = invitationService.revokeInvitation(alice.id(), channel.id(), 999L);

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<InvitationError, String>) result).value()).isInstanceOf(InvitationError.InvitationNotFound.class);
    }

    @Test
    void testRevokeInvitation_ChannelNotFound() {
        Either<InvitationError, String> result = invitationService.revokeInvitation(alice.id(), 999L, 1L);

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<InvitationError, String>) result).value()).isInstanceOf(InvitationError.ChannelNotFound.class);
    }

    @Test
    void testGetInvitationsForChannel_UserNotAuthorized() {
        Either<InvitationError, List<Invitation>> result = invitationService.getInvitationsForChannel(charlie.id(), channel.id());

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<InvitationError, List<Invitation>>) result).value()).isInstanceOf(InvitationError.UserNotAuthorized.class);
    }

    @Test
    void testRevokeInvitation_UserNotFound() {
        Either<InvitationError, String> result = invitationService.revokeInvitation(999L, channel.id(), 1L);

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<InvitationError, String>) result).value()).isInstanceOf(InvitationError.UserNotFound.class);
    }
}