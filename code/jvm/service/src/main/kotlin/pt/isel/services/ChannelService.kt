package pt.isel.services

import jakarta.inject.Named
import pt.isel.domain.channel.AccessType
import pt.isel.domain.channel.Channel
import pt.isel.domain.channel.ChannelMember
import pt.isel.domain.invitation.InvitationStatus
import pt.isel.domain.user.User
import pt.isel.domain.user.UserInfo
import pt.isel.repositories.Transaction
import pt.isel.repositories.TransactionManager
import java.time.LocalDateTime

@Named
class ChannelService(
    private val trxManager: TransactionManager,
) {
    fun createChannel(
        name: String,
        ownerId: Long,
        isPublic: Boolean,
    ): Either<ChannelError, Channel> {
        if (name.isBlank()) return failure(ChannelError.EmptyChannelName)
        if (name.length !in 1..30) return failure(ChannelError.InvalidChannelNameLength)

        return trxManager.run {
            val owner = repoUsers.findById(ownerId) ?: return@run failure(ChannelError.UserNotFound)

            if (repoChannels.findByName(name) != null) {
                return@run failure(ChannelError.ChannelAlreadyExists)
            }
            val ownerInfo = UserInfo(owner.id, owner.username)
            val newChannel = repoChannels.create(name, ownerInfo, isPublic)
            repoMemberships.addUserToChannel(ownerInfo, newChannel, AccessType.READ_WRITE)
            success(newChannel)
        }
    }

    fun getChannelById(channelId: Long): Either<ChannelError, Channel> =
        trxManager.run {
            repoChannels.findById(channelId)?.let { success(it) }
                ?: return@run failure(ChannelError.ChannelNotFound)
        }

    fun deleteChannel(
        ownerId: Long,
        channelId: Long,
    ): Either<ChannelError, String> =
        trxManager.run {
            when (val result = checkUserIsOwner(ownerId, channelId)) {
                is Failure -> {
                    result
                }

                is Success -> {
                    val (_, channel) = result.value
                    repoChannels.deleteById(channel.id)
                    success("Channel '${channel.name}' was deleted successfully.")
                }
            }
        }

    fun getJoinedChannels(
        userId: Long,
        limit: Int = 50,
        offset: Int = 0,
    ): Either<ChannelError, List<Channel>> {
        if (limit <= 0) return failure(ChannelError.InvalidLimit)
        if (offset < 0) return failure(ChannelError.InvalidOffset)

        return trxManager.run {
            if (repoUsers.findById(userId) == null) return@run failure(ChannelError.UserNotFound)
            val channels =
                repoMemberships.findAllChannelsForUser(userId, limit, offset).map { it.channel }
            success(channels)
        }
    }

    fun getUsersInChannel(
        channelId: Long,
        limit: Int = 50,
        offset: Int = 0,
    ): Either<ChannelError, List<UserInfo>> {
        if (limit <= 0) return failure(ChannelError.InvalidLimit)
        if (offset < 0) return failure(ChannelError.InvalidOffset)

        return trxManager.run {
            val users =
                repoMemberships.findAllMembersInChannel(channelId, limit, offset).map { it.user }
            success(users)
        }
    }

    fun editChannel(
        ownerId: Long,
        channelId: Long,
        name: String,
        isPublic: Boolean,
    ): Either<ChannelError, Channel> {
        if (name.isBlank()) return failure(ChannelError.EmptyChannelName)
        if (name.length !in 1..30) return failure(ChannelError.InvalidChannelNameLength)

        return trxManager.run {
            when (val result = checkUserIsOwner(ownerId, channelId)) {
                is Failure -> {
                    result
                }

                is Success -> {
                    val (_, channel) = result.value
                    val updatedChannel = channel.copy(name = name, isPublic = isPublic)
                    repoChannels.save(updatedChannel)
                    success(updatedChannel)
                }
            }
        }
    }

    fun getAccessType(
        userId: Long,
        channelId: Long,
    ): Either<MessageError, AccessType> {
        return trxManager.run {
            val membership =
                repoMemberships.findUserInChannel(userId, channelId)
                    ?: return@run failure(MessageError.UserNotInChannel)

            success(membership.accessType)
        }
    }

    fun editMemberAccess(
        ownerId: Long,
        channelId: Long,
        userId: Long,
        accessType: AccessType,
    ): Either<ChannelError, ChannelMember> =
        trxManager.run {
            when (val result = checkUserCanEditMember(ownerId, channelId, userId)) {
                is Failure -> {
                    result
                }

                is Success -> {
                    val (_, _, userMembership) = result.value
                    val updatedMembership = userMembership.copy(accessType = accessType)
                    repoMemberships.save(updatedMembership)
                    success(updatedMembership)
                }
            }
        }

    fun searchChannels(
        query: String,
        limit: Int = 50,
        offset: Int = 0,
    ): Either<ChannelError, List<Channel>> {
        if (limit <= 0) return failure(ChannelError.InvalidLimit)
        if (offset < 0) return failure(ChannelError.InvalidOffset)

        return trxManager.run {
            val channels =
                if (query.isBlank()) {
                    repoChannels.findAllPublicChannels(limit, offset)
                } else {
                    repoChannels.searchByName(query, limit, offset).filter {
                        it.name.contains(query, ignoreCase = true)
                    }
                }
            success(channels)
        }
    }

    fun joinPublicChannel(
        userId: Long,
        channelId: Long,
    ): Either<ChannelError, String> =
        trxManager.run {
            val user = repoUsers.findById(userId) ?: return@run failure(ChannelError.UserNotFound)
            val channel =
                repoChannels.findById(channelId) ?: return@run failure(ChannelError.ChannelNotFound)

            if (repoMemberships.findUserInChannel(user.id, channel.id) != null) {
                return@run failure(ChannelError.UserAlreadyInChannel)
            }
            val userInfo = UserInfo(user.id, user.username)
            repoMemberships.addUserToChannel(userInfo, channel, AccessType.READ_WRITE)
            return@run success("Joined public channel '${channel.name}'.")
        }

    fun leaveChannel(
        channelId: Long,
        userId: Long,
    ): Either<ChannelError, String> {
        return trxManager.run {
            val channel =
                repoChannels.findById(channelId) ?: return@run failure(ChannelError.ChannelNotFound)
            val membership =
                repoMemberships.findUserInChannel(userId, channelId)
                    ?: return@run failure(ChannelError.UserNotInChannel)

            if (channel.owner.id == userId) {
                return@run failure(ChannelError.OwnerCannotLeave)
            }

            repoMemberships.deleteById(membership.id)
            success("Left channel '${channel.name}'")
        }
    }

    fun joinPrivateChannel(
        userId: Long,
        token: String,
    ): Either<ChannelError, String> =
        trxManager.run {
            val user = repoUsers.findById(userId) ?: return@run failure(ChannelError.UserNotFound)
            val invitation =
                repoInvitations.findByToken(token)
                    ?: return@run failure(ChannelError.TokenNotFound)

            if (invitation.expiresAt.isBefore(LocalDateTime.now())) {
                return@run failure(ChannelError.InvitationExpired)
            }
            if (invitation.status != InvitationStatus.PENDING) {
                return@run failure(ChannelError.InvitationAlreadyUsed)
            }

            val channel =
                repoChannels.findById(invitation.channel.id)
                    ?: return@run failure(ChannelError.ChannelNotFound)

            if (repoMemberships.findUserInChannel(user.id, channel.id) != null) {
                return@run failure(ChannelError.UserAlreadyInChannel)
            }
            val userInfo = UserInfo(user.id, user.username)
            repoMemberships.addUserToChannel(userInfo, channel, invitation.accessType)
            repoInvitations.save(invitation.copy(status = InvitationStatus.ACCEPTED))
            success("Joined private channel '${channel.name}'")
        }

    private fun Transaction.checkUserIsOwner(
        userId: Long,
        channelId: Long,
    ): Either<ChannelError, Pair<User, Channel>> {
        val user = repoUsers.findById(userId) ?: return failure(ChannelError.UserNotFound)
        val channel =
            repoChannels.findById(channelId) ?: return failure(ChannelError.ChannelNotFound)
        if (channel.owner.id != user.id) {
            return failure(ChannelError.UserNotOwner)
        }
        return success(user to channel)
    }

    private fun Transaction.checkUserCanEditMember(
        ownerId: Long,
        channelId: Long,
        userId: Long,
    ): Either<ChannelError, Triple<User, User, ChannelMember>> {
        val owner = repoUsers.findById(ownerId) ?: return failure(ChannelError.UserNotFound)
        val userToEdit = repoUsers.findById(userId) ?: return failure(ChannelError.UserNotFound)
        val channel =
            repoChannels.findById(channelId) ?: return failure(ChannelError.ChannelNotFound)

        val ownerMembership =
            repoMemberships.findUserInChannel(owner.id, channel.id)
                ?: return failure(ChannelError.UserNotInChannel)
        val userMembership =
            repoMemberships.findUserInChannel(userToEdit.id, channel.id)
                ?: return failure(ChannelError.UserNotInChannel)

        if (channel.owner.id != owner.id) return failure(ChannelError.UserNotOwner)
        if (userToEdit.id == channel.owner.id) return failure(ChannelError.UserIsOwner)
        if (ownerMembership.accessType == AccessType.READ_ONLY) return failure(ChannelError.UserNotAuthorized)

        return success(Triple(owner, userToEdit, userMembership))
    }
}
