package pt.isel.services.invitations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.common.Either;
import pt.isel.domain.common.EitherAssert;
import pt.isel.domain.common.InvitationError;
import pt.isel.domain.invitations.Invitation;
import pt.isel.domain.invitations.InvitationStatus;
import pt.isel.domain.users.User;
import pt.isel.services.AbstractServiceTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InvitationServiceTest extends AbstractServiceTest {

    private InvitationService invitationService;
    private Channel channel;

    @BeforeEach
    void setUp() {
        super.setUpBaseState();
        invitationService = new InvitationService(trxManager, clock);
        channel = createChannelWithMembers("Secret", false);
    }

    @Test
    void testCreateInvitation_SuccessByOwner() {
        LocalDateTime expiry = LocalDateTime.now(clock).plusDays(1);
        Either<InvitationError, Invitation> result = invitationService.createInvitation(alice.id(), channel.id(), AccessType.READ_ONLY, expiry);

        Invitation inv = EitherAssert.assertRight(result);
        assertThat(inv.token()).isNotNull();
        assertThat(inv.accessType()).isEqualTo(AccessType.READ_ONLY);
    }

    @Test
    void testCreateInvitation_SuccessByReadWriteMember() {
        LocalDateTime expiry = LocalDateTime.now(clock).plusDays(1);
        Either<InvitationError, Invitation> result = invitationService.createInvitation(bob.id(), channel.id(), AccessType.READ_WRITE, expiry);

        EitherAssert.assertRight(result);
    }

    @Test
    void testCreateInvitation_FailureByReadOnlyMember() {
        LocalDateTime expiry = LocalDateTime.now(clock).plusDays(1);
        Either<InvitationError, Invitation> result = invitationService.createInvitation(charlie.id(), channel.id(), AccessType.READ_ONLY, expiry);

        EitherAssert.assertLeft(result, InvitationError.UserNotAuthorized.class);
    }

    @Test
    void testCreateInvitation_InvalidExpiration() {
        LocalDateTime expiry = LocalDateTime.now(clock).minusDays(1);
        Either<InvitationError, Invitation> result = invitationService.createInvitation(alice.id(), channel.id(), AccessType.READ_ONLY, expiry);

        EitherAssert.assertLeft(result, InvitationError.InvalidExpirationTime.class);
    }

    @Test
    void testGetInvitationsForChannel_Success() {
        invitationService.createInvitation(alice.id(), channel.id(), AccessType.READ_ONLY, LocalDateTime.now(clock).plusDays(1));
        invitationService.createInvitation(bob.id(), channel.id(), AccessType.READ_WRITE, LocalDateTime.now(clock).plusDays(1));

        Either<InvitationError, List<Invitation>> result = invitationService.getInvitationsForChannel(alice.id(), channel.id());

        assertThat(EitherAssert.assertRight(result)).hasSize(2);
    }

    @Test
    void testGetInvitationsForChannel_UserNotFound() {
        Either<InvitationError, List<Invitation>> result = invitationService.getInvitationsForChannel(999L, channel.id());

        EitherAssert.assertLeft(result, InvitationError.UserNotFound.class);
    }

    @Test
    void testGetInvitationsForChannel_ChannelNotFound() {
        Either<InvitationError, List<Invitation>> result = invitationService.getInvitationsForChannel(alice.id(), 999L);

        EitherAssert.assertLeft(result, InvitationError.ChannelNotFound.class);
    }

    @Test
    void testGetInvitationsForChannel_UserNotInChannel() {
        User dave = trxManager.run(trx -> insertUser(trx, "dave"));
        Either<InvitationError, List<Invitation>> result = invitationService.getInvitationsForChannel(dave.id(), channel.id());

        EitherAssert.assertLeft(result, InvitationError.UserNotInChannel.class);
    }

    @Test
    void testRevokeInvitation_SuccessByOwner() {
        Invitation inv = EitherAssert.assertRight(invitationService.createInvitation(
                alice.id(), channel.id(), AccessType.READ_ONLY, LocalDateTime.now(clock).plusDays(1)
        ));

        Either<InvitationError, String> result = invitationService.revokeInvitation(alice.id(), channel.id(), inv.id());
        EitherAssert.assertRight(result);

        trxManager.run(trx -> {
            assertThat(trx.repoInvitations().findById(inv.id()).status()).isEqualTo(InvitationStatus.REJECTED);
            return null;
        });
    }

    @Test
    void testRevokeInvitation_FailureNotOwner() {
        Invitation inv = EitherAssert.assertRight(invitationService.createInvitation(
                alice.id(), channel.id(), AccessType.READ_ONLY, LocalDateTime.now(clock).plusDays(1)
        ));

        Either<InvitationError, String> result = invitationService.revokeInvitation(bob.id(), channel.id(), inv.id());

        EitherAssert.assertLeft(result, InvitationError.UserNotAuthorized.class);
    }

    @Test
    void testCreateInvitation_UserNotFound() {
        Either<InvitationError, Invitation> result = invitationService.createInvitation(999L, channel.id(), AccessType.READ_ONLY, LocalDateTime.now(clock).plusDays(1));

        EitherAssert.assertLeft(result, InvitationError.UserNotFound.class);
    }

    @Test
    void testCreateInvitation_ChannelNotFound() {
        Either<InvitationError, Invitation> result = invitationService.createInvitation(alice.id(), 999L, AccessType.READ_ONLY, LocalDateTime.now(clock).plusDays(1));

        EitherAssert.assertLeft(result, InvitationError.ChannelNotFound.class);
    }

    @Test
    void testCreateInvitation_UserNotInChannel() {
        User dave = trxManager.run(trx -> insertUser(trx, "dave"));
        Either<InvitationError, Invitation> result = invitationService.createInvitation(dave.id(), channel.id(), AccessType.READ_ONLY, LocalDateTime.now(clock).plusDays(1));

        EitherAssert.assertLeft(result, InvitationError.UserNotInChannel.class);
    }

    @Test
    void testRevokeInvitation_InvitationNotFound() {
        Either<InvitationError, String> result = invitationService.revokeInvitation(alice.id(), channel.id(), 999L);

        EitherAssert.assertLeft(result, InvitationError.InvitationNotFound.class);
    }

    @Test
    void testRevokeInvitation_ChannelNotFound() {
        Either<InvitationError, String> result = invitationService.revokeInvitation(alice.id(), 999L, 1L);

        EitherAssert.assertLeft(result, InvitationError.ChannelNotFound.class);
    }

    @Test
    void testGetInvitationsForChannel_UserNotAuthorized() {
        Either<InvitationError, List<Invitation>> result = invitationService.getInvitationsForChannel(charlie.id(), channel.id());

        EitherAssert.assertLeft(result, InvitationError.UserNotAuthorized.class);
    }

    @Test
    void testRevokeInvitation_UserNotFound() {
        Either<InvitationError, String> result = invitationService.revokeInvitation(999L, channel.id(), 1L);

        EitherAssert.assertLeft(result, InvitationError.UserNotFound.class);
    }
}