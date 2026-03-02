package pt.isel.services.channels;

import jakarta.inject.Named;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.channels.ChannelMember;
import pt.isel.domain.common.ChannelError;
import pt.isel.domain.common.Either;
import pt.isel.domain.common.MessageError;
import pt.isel.domain.invitations.Invitation;
import pt.isel.domain.invitations.InvitationStatus;
import pt.isel.domain.users.User;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.Transaction;
import pt.isel.repositories.TransactionManager;

import java.time.LocalDateTime;
import java.util.List;

@Named
public class ChannelService {
    private final TransactionManager trxManager;

    public ChannelService(TransactionManager trxManager) {
        this.trxManager = trxManager;
    }

    public Either<ChannelError, Channel> createChannel(String name, Long ownerId, boolean isPublic) {
        if (name == null || name.isBlank()) return Either.failure(new ChannelError.EmptyChannelName());
        if (name.length() < 1 || name.length() > 30) return Either.failure(new ChannelError.InvalidChannelNameLength());

        return trxManager.run(trx -> {
            User owner = trx.repoUsers().findById(ownerId);
            if (owner == null) return Either.failure(new ChannelError.UserNotFound());

            if (trx.repoChannels().findByName(name) != null) {
                return Either.failure(new ChannelError.ChannelAlreadyExists());
            }

            UserInfo ownerInfo = new UserInfo(owner.id(), owner.username());
            Channel newChannel = trx.repoChannels().create(name, ownerInfo, isPublic);
            trx.repoMemberships().addUserToChannel(ownerInfo, newChannel, AccessType.READ_WRITE);
            return Either.success(newChannel);
        });
    }

    public Either<ChannelError, Channel> getChannelById(Long channelId) {
        return trxManager.run(trx -> {
            Channel channel = trx.repoChannels().findById(channelId);
            return channel != null ? Either.success(channel) : Either.failure(new ChannelError.ChannelNotFound());
        });
    }

    public Either<ChannelError, String> deleteChannel(Long ownerId, Long channelId) {
        return trxManager.run(trx -> {
            return switch (checkUserIsOwner(trx, ownerId, channelId)) {
                case Either.Left<ChannelError, UserChannelPair> left -> Either.failure(left.value());
                case Either.Right<ChannelError, UserChannelPair> right -> {
                    trx.repoChannels().deleteById(right.value().channel().id());
                    yield Either.success("Channel '" + right.value().channel().name() + "' was deleted successfully.");
                }
            };
        });
    }

    public Either<ChannelError, List<Channel>> getJoinedChannels(Long userId, int limit, int offset) {
        if (limit <= 0) return Either.failure(new ChannelError.InvalidLimit());
        if (offset < 0) return Either.failure(new ChannelError.InvalidOffset());

        return trxManager.run(trx -> {
            if (trx.repoUsers().findById(userId) == null) return Either.failure(new ChannelError.UserNotFound());
            List<Channel> channels = trx.repoMemberships().findAllChannelsForUser(userId, limit, offset).stream().map(ChannelMember::channel).toList();
            return Either.success(channels);
        });
    }

    public Either<ChannelError, List<UserInfo>> getUsersInChannel(Long channelId, int limit, int offset) {
        if (limit <= 0) return Either.failure(new ChannelError.InvalidLimit());
        if (offset < 0) return Either.failure(new ChannelError.InvalidOffset());

        return trxManager.run(trx -> {
            List<UserInfo> users = trx.repoMemberships().findAllMembersInChannel(channelId, limit, offset).stream().map(ChannelMember::user).toList();
            return Either.success(users);
        });
    }

    public Either<ChannelError, Channel> editChannel(Long ownerId, Long channelId, String name, boolean isPublic) {
        if (name == null || name.isBlank()) return Either.failure(new ChannelError.EmptyChannelName());
        if (name.length() < 1 || name.length() > 30) return Either.failure(new ChannelError.InvalidChannelNameLength());

        return trxManager.run(trx -> {
            return switch (checkUserIsOwner(trx, ownerId, channelId)) {
                case Either.Left<ChannelError, UserChannelPair> left -> Either.failure(left.value());
                case Either.Right<ChannelError, UserChannelPair> right -> {
                    Channel channel = right.value().channel();
                    Channel updatedChannel = new Channel(channel.id(), name, channel.owner(), isPublic);
                    trx.repoChannels().save(updatedChannel);
                    yield Either.success(updatedChannel);
                }
            };
        });
    }

    public Either<MessageError, AccessType> getAccessType(Long requesterId, Long targetUserId, Long channelId) {
        return trxManager.run(trx -> {
            var targetMembership = trx.repoMemberships().findUserInChannel(targetUserId, channelId);
            if (targetMembership == null) return Either.failure(new MessageError.UserNotInChannel());

            if (!requesterId.equals(targetUserId)) {
                if (trx.repoMemberships().findUserInChannel(requesterId, channelId) == null) {
                    return Either.failure(new MessageError.UserNotAuthorized());
                }
            }
            return Either.success(targetMembership.accessType());
        });
    }

    public Either<ChannelError, ChannelMember> editMemberAccess(Long ownerId, Long channelId, Long userId, AccessType accessType) {
        return trxManager.run(trx -> {
            return switch (checkUserCanEditMember(trx, ownerId, channelId, userId)) {
                case Either.Left<ChannelError, MemberEditTriple> left -> Either.failure(left.value());
                case Either.Right<ChannelError, MemberEditTriple> right -> {
                    var userMembership = right.value().userMembership();
                    ChannelMember updatedMembership = new ChannelMember(userMembership.id(), userMembership.user(), userMembership.channel(), accessType);
                    trx.repoMemberships().save(updatedMembership);
                    yield Either.success(updatedMembership);
                }
            };
        });
    }

    public Either<ChannelError, List<Channel>> searchChannels(String query, int limit, int offset) {
        if (limit <= 0) return Either.failure(new ChannelError.InvalidLimit());
        if (offset < 0) return Either.failure(new ChannelError.InvalidOffset());

        return trxManager.run(trx -> {
            List<Channel> channels = (query == null || query.isBlank())
                    ? trx.repoChannels().findAllPublicChannels(limit, offset)
                    : trx.repoChannels().searchByName(query, limit, offset);
            return Either.success(channels);
        });
    }

    public Either<ChannelError, String> joinPublicChannel(Long userId, Long channelId) {
        return trxManager.run(trx -> {
            User user = trx.repoUsers().findById(userId);
            if (user == null) return Either.failure(new ChannelError.UserNotFound());

            Channel channel = trx.repoChannels().findById(channelId);
            if (channel == null) return Either.failure(new ChannelError.ChannelNotFound());

            if (!channel.isPublic()) return Either.failure(new ChannelError.ChannelIsPrivate());

            if (trx.repoMemberships().findUserInChannel(user.id(), channel.id()) != null) {
                return Either.failure(new ChannelError.UserAlreadyInChannel());
            }

            trx.repoMemberships().addUserToChannel(new UserInfo(user.id(), user.username()), channel, AccessType.READ_WRITE);
            return Either.success("Joined public channel '" + channel.name() + "'.");
        });
    }

    public Either<ChannelError, String> joinPrivateChannel(Long userId, String token) {
        return trxManager.run(trx -> {
            User user = trx.repoUsers().findById(userId);
            if (user == null) return Either.failure(new ChannelError.UserNotFound());

            var invitation = trx.repoInvitations().findByToken(token);
            if (invitation == null) return Either.failure(new ChannelError.TokenNotFound());

            if (invitation.expiresAt().isBefore(LocalDateTime.now()))
                return Either.failure(new ChannelError.InvitationExpired());
            if (invitation.status() != InvitationStatus.PENDING)
                return Either.failure(new ChannelError.InvitationAlreadyUsed());

            Channel channel = trx.repoChannels().findById(invitation.channel().id());
            if (channel == null) return Either.failure(new ChannelError.ChannelNotFound());

            if (trx.repoMemberships().findUserInChannel(user.id(), channel.id()) != null) {
                return Either.failure(new ChannelError.UserAlreadyInChannel());
            }

            trx.repoMemberships().addUserToChannel(new UserInfo(user.id(), user.username()), channel, invitation.accessType());
            trx.repoInvitations().save(new Invitation(invitation.id(), invitation.token(), invitation.createdBy(), invitation.channel(), invitation.accessType(), invitation.expiresAt(), InvitationStatus.ACCEPTED));
            return Either.success("Joined private channel '" + channel.name() + "'");
        });
    }

    public Either<ChannelError, String> leaveChannel(Long channelId, Long userId) {
        return trxManager.run(trx -> {
            Channel channel = trx.repoChannels().findById(channelId);
            if (channel == null) return Either.failure(new ChannelError.ChannelNotFound());

            ChannelMember membership = trx.repoMemberships().findUserInChannel(userId, channelId);
            if (membership == null) return Either.failure(new ChannelError.UserNotInChannel());

            if (channel.owner().id().equals(userId)) return Either.failure(new ChannelError.OwnerCannotLeave());

            trx.repoMemberships().deleteById(membership.id());
            return Either.success("Left channel '" + channel.name() + "'");
        });
    }

    private Either<ChannelError, UserChannelPair> checkUserIsOwner(Transaction trx, Long userId, Long channelId) {
        User user = trx.repoUsers().findById(userId);
        if (user == null) return Either.failure(new ChannelError.UserNotFound());

        Channel channel = trx.repoChannels().findById(channelId);
        if (channel == null) return Either.failure(new ChannelError.ChannelNotFound());

        if (!channel.owner().id().equals(user.id())) return Either.failure(new ChannelError.UserNotOwner());

        return Either.success(new UserChannelPair(user, channel));
    }

    private Either<ChannelError, MemberEditTriple> checkUserCanEditMember(Transaction trx, Long ownerId, Long channelId, Long userId) {
        User owner = trx.repoUsers().findById(ownerId);
        if (owner == null) return Either.failure(new ChannelError.UserNotFound());

        User userToEdit = trx.repoUsers().findById(userId);
        if (userToEdit == null) return Either.failure(new ChannelError.UserNotFound());

        Channel channel = trx.repoChannels().findById(channelId);
        if (channel == null) return Either.failure(new ChannelError.ChannelNotFound());

        ChannelMember ownerMembership = trx.repoMemberships().findUserInChannel(owner.id(), channel.id());
        if (ownerMembership == null) return Either.failure(new ChannelError.UserNotInChannel());

        ChannelMember userMembership = trx.repoMemberships().findUserInChannel(userToEdit.id(), channel.id());
        if (userMembership == null) return Either.failure(new ChannelError.UserNotInChannel());

        if (!channel.owner().id().equals(owner.id())) return Either.failure(new ChannelError.UserNotOwner());
        if (userToEdit.id().equals(channel.owner().id())) return Either.failure(new ChannelError.UserIsOwner());
        if (ownerMembership.accessType() == AccessType.READ_ONLY)
            return Either.failure(new ChannelError.UserNotAuthorized());

        return Either.success(new MemberEditTriple(owner, userToEdit, userMembership));
    }

    private record UserChannelPair(User user, Channel channel) {
    }

    private record MemberEditTriple(User owner, User userToEdit, ChannelMember userMembership) {
    }
}