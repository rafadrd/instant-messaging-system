package pt.isel.services.invitations;

import jakarta.inject.Named;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.common.Either;
import pt.isel.domain.common.InvitationError;
import pt.isel.domain.invitations.Invitation;
import pt.isel.domain.invitations.InvitationStatus;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.Transaction;
import pt.isel.repositories.TransactionManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Named
public class InvitationService {
    private final TransactionManager trxManager;
    private final Clock clock;

    public InvitationService(TransactionManager trxManager, Clock clock) {
        this.trxManager = trxManager;
        this.clock = clock;
    }

    public Either<InvitationError, Invitation> createInvitation(Long creatorId, Long channelId, AccessType accessType, LocalDateTime expiresAt) {
        if (expiresAt.isBefore(LocalDateTime.now(clock))) {
            return Either.failure(new InvitationError.InvalidExpirationTime());
        }

        return trxManager.run(trx -> checkUserCanManageInvitations(trx, creatorId, channelId)
                .flatMap(pair -> {
                    String token = UUID.randomUUID().toString();
                    var invitation = trx.repoInvitations().create(token, pair.creator(), pair.channel(), accessType, expiresAt);
                    return Either.success(invitation);
                }));
    }

    public Either<InvitationError, List<Invitation>> getInvitationsForChannel(Long requesterId, Long channelId) {
        return trxManager.run(trx -> checkUserCanManageInvitations(trx, requesterId, channelId)
                .flatMap(pair -> {
                    List<Invitation> invitations = trx.repoInvitations().findByChannelId(channelId);
                    return Either.success(invitations);
                }));
    }

    public Either<InvitationError, String> revokeInvitation(Long userId, Long channelId, Long invitationId) {
        return trxManager.run(trx -> {
            var user = trx.repoUsers().findById(userId);
            if (user == null) return Either.failure(new InvitationError.UserNotFound());

            var channel = trx.repoChannels().findById(channelId);
            if (channel == null) return Either.failure(new InvitationError.ChannelNotFound());

            if (!user.id().equals(channel.owner().id())) {
                return Either.failure(new InvitationError.UserNotAuthorized());
            }

            var invitation = trx.repoInvitations().findById(invitationId);
            if (invitation == null) return Either.failure(new InvitationError.InvitationNotFound());

            trx.repoInvitations().save(new Invitation(invitation.id(), invitation.token(), invitation.createdBy(), invitation.channel(), invitation.accessType(), invitation.expiresAt(), InvitationStatus.REJECTED));
            return Either.success("Invitation revoked.");
        });
    }

    private Either<InvitationError, CreatorChannelPair> checkUserCanManageInvitations(Transaction trx, Long creatorId, Long channelId) {
        var creator = trx.repoUsers().findById(creatorId);
        if (creator == null) return Either.failure(new InvitationError.UserNotFound());

        var channel = trx.repoChannels().findById(channelId);
        if (channel == null) return Either.failure(new InvitationError.ChannelNotFound());

        var membership = trx.repoMemberships().findUserInChannel(creator.id(), channel.id());
        if (membership == null) return Either.failure(new InvitationError.UserNotInChannel());

        if (membership.accessType() != AccessType.READ_WRITE) {
            return Either.failure(new InvitationError.UserNotAuthorized());
        }

        UserInfo creatorInfo = new UserInfo(creator.id(), creator.username());
        return Either.success(new CreatorChannelPair(creatorInfo, channel));
    }

    private record CreatorChannelPair(UserInfo creator, Channel channel) {
    }
}