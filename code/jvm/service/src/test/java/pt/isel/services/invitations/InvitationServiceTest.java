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
    void CreateInvitation_ByOwner_ReturnsSuccess() {
        LocalDateTime expiry = LocalDateTime.now(clock).plusDays(1);

        Either<InvitationError, Invitation> result = invitationService.createInvitation(alice.id(), channel.id(), AccessType.READ_ONLY, expiry);

        Invitation inv = EitherAssert.assertThat(result).isRight().getRightValue();
        assertThat(inv.token()).isNotNull();
        assertThat(inv.accessType()).isEqualTo(AccessType.READ_ONLY);
    }

    @Test
    void CreateInvitation_ByReadWriteMember_ReturnsSuccess() {
        LocalDateTime expiry = LocalDateTime.now(clock).plusDays(1);

        Either<InvitationError, Invitation> result = invitationService.createInvitation(bob.id(), channel.id(), AccessType.READ_WRITE, expiry);

        Invitation inv = EitherAssert.assertThat(result).isRight().getRightValue();
        assertThat(inv.accessType()).isEqualTo(AccessType.READ_WRITE);
    }

    @Test
    void CreateInvitation_ByReadOnlyMember_ReturnsLeft() {
        LocalDateTime expiry = LocalDateTime.now(clock).plusDays(1);

        Either<InvitationError, Invitation> result = invitationService.createInvitation(charlie.id(), channel.id(), AccessType.READ_ONLY, expiry);

        EitherAssert.assertThat(result).isLeftInstanceOf(InvitationError.UserNotAuthorized.class);
    }

    @Test
    void CreateInvitation_InvalidExpiration_ReturnsLeft() {
        LocalDateTime expiry = LocalDateTime.now(clock).minusDays(1);

        Either<InvitationError, Invitation> result = invitationService.createInvitation(alice.id(), channel.id(), AccessType.READ_ONLY, expiry);

        EitherAssert.assertThat(result).isLeftInstanceOf(InvitationError.InvalidExpirationTime.class);
    }

    @Test
    void GetInvitationsForChannel_ValidInput_ReturnsSuccess() {
        invitationService.createInvitation(alice.id(), channel.id(), AccessType.READ_ONLY, LocalDateTime.now(clock).plusDays(1));
        invitationService.createInvitation(bob.id(), channel.id(), AccessType.READ_WRITE, LocalDateTime.now(clock).plusDays(1));

        Either<InvitationError, List<Invitation>> result = invitationService.getInvitationsForChannel(alice.id(), channel.id());

        List<Invitation> invitations = EitherAssert.assertThat(result).isRight().getRightValue();
        assertThat(invitations).hasSize(2);
    }

    @Test
    void GetInvitationsForChannel_UserNotFound_ReturnsLeft() {
        Either<InvitationError, List<Invitation>> result = invitationService.getInvitationsForChannel(999L, channel.id());

        EitherAssert.assertThat(result).isLeftInstanceOf(InvitationError.UserNotFound.class);
    }

    @Test
    void GetInvitationsForChannel_ChannelNotFound_ReturnsLeft() {
        Either<InvitationError, List<Invitation>> result = invitationService.getInvitationsForChannel(alice.id(), 999L);

        EitherAssert.assertThat(result).isLeftInstanceOf(InvitationError.ChannelNotFound.class);
    }

    @Test
    void GetInvitationsForChannel_UserNotInChannel_ReturnsLeft() {
        User dave = trxManager.run(trx -> insertUser(trx, "dave"));

        Either<InvitationError, List<Invitation>> result = invitationService.getInvitationsForChannel(dave.id(), channel.id());

        EitherAssert.assertThat(result).isLeftInstanceOf(InvitationError.UserNotInChannel.class);
    }

    @Test
    void RevokeInvitation_ByOwner_ReturnsSuccess() {
        Invitation inv = EitherAssert.assertThat(invitationService.createInvitation(
                alice.id(), channel.id(), AccessType.READ_ONLY, LocalDateTime.now(clock).plusDays(1)
        )).isRight().getRightValue();

        Either<InvitationError, String> result = invitationService.revokeInvitation(alice.id(), channel.id(), inv.id());

        EitherAssert.assertThat(result).containsRight("Invitation revoked.");
        trxManager.run(trx -> {
            assertThat(trx.repoInvitations().findById(inv.id()).status()).isEqualTo(InvitationStatus.REJECTED);
            return null;
        });
    }

    @Test
    void RevokeInvitation_NotOwner_ReturnsLeft() {
        Invitation inv = EitherAssert.assertThat(invitationService.createInvitation(
                alice.id(), channel.id(), AccessType.READ_ONLY, LocalDateTime.now(clock).plusDays(1)
        )).isRight().getRightValue();

        Either<InvitationError, String> result = invitationService.revokeInvitation(bob.id(), channel.id(), inv.id());

        EitherAssert.assertThat(result).isLeftInstanceOf(InvitationError.UserNotAuthorized.class);
    }

    @Test
    void CreateInvitation_UserNotFound_ReturnsLeft() {
        Either<InvitationError, Invitation> result = invitationService.createInvitation(999L, channel.id(), AccessType.READ_ONLY, LocalDateTime.now(clock).plusDays(1));

        EitherAssert.assertThat(result).isLeftInstanceOf(InvitationError.UserNotFound.class);
    }

    @Test
    void CreateInvitation_ChannelNotFound_ReturnsLeft() {
        Either<InvitationError, Invitation> result = invitationService.createInvitation(alice.id(), 999L, AccessType.READ_ONLY, LocalDateTime.now(clock).plusDays(1));

        EitherAssert.assertThat(result).isLeftInstanceOf(InvitationError.ChannelNotFound.class);
    }

    @Test
    void CreateInvitation_UserNotInChannel_ReturnsLeft() {
        User dave = trxManager.run(trx -> insertUser(trx, "dave"));

        Either<InvitationError, Invitation> result = invitationService.createInvitation(dave.id(), channel.id(), AccessType.READ_ONLY, LocalDateTime.now(clock).plusDays(1));

        EitherAssert.assertThat(result).isLeftInstanceOf(InvitationError.UserNotInChannel.class);
    }

    @Test
    void RevokeInvitation_InvitationNotFound_ReturnsLeft() {
        Either<InvitationError, String> result = invitationService.revokeInvitation(alice.id(), channel.id(), 999L);

        EitherAssert.assertThat(result).isLeftInstanceOf(InvitationError.InvitationNotFound.class);
    }

    @Test
    void RevokeInvitation_ChannelNotFound_ReturnsLeft() {
        Either<InvitationError, String> result = invitationService.revokeInvitation(alice.id(), 999L, 1L);

        EitherAssert.assertThat(result).isLeftInstanceOf(InvitationError.ChannelNotFound.class);
    }

    @Test
    void GetInvitationsForChannel_UserNotAuthorized_ReturnsLeft() {
        Either<InvitationError, List<Invitation>> result = invitationService.getInvitationsForChannel(charlie.id(), channel.id());

        EitherAssert.assertThat(result).isLeftInstanceOf(InvitationError.UserNotAuthorized.class);
    }

    @Test
    void RevokeInvitation_UserNotFound_ReturnsLeft() {
        Either<InvitationError, String> result = invitationService.revokeInvitation(999L, channel.id(), 1L);

        EitherAssert.assertThat(result).isLeftInstanceOf(InvitationError.UserNotFound.class);
    }
}