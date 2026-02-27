package pt.isel.services.invitations

import jakarta.inject.Named
import pt.isel.domain.channels.AccessType
import pt.isel.domain.channels.Channel
import pt.isel.domain.invitations.Invitation
import pt.isel.domain.invitations.InvitationStatus
import pt.isel.domain.users.UserInfo
import pt.isel.repositories.Transaction
import pt.isel.repositories.TransactionManager
import pt.isel.services.common.Either
import pt.isel.services.common.Failure
import pt.isel.services.common.InvitationError
import pt.isel.services.common.Success
import pt.isel.services.common.failure
import pt.isel.services.common.success
import java.time.LocalDateTime
import java.util.UUID

@Named
class InvitationService(
    private val trxManager: TransactionManager,
) {
    fun createInvitation(
        creatorId: Long,
        channelId: Long,
        accessType: AccessType,
        expiresAt: LocalDateTime,
    ): Either<InvitationError, Invitation> {
        if (expiresAt.isBefore(LocalDateTime.now())) {
            return failure(InvitationError.InvalidExpirationTime)
        }

        return trxManager.run {
            when (val checkResult = checkUserCanManageInvitations(creatorId, channelId)) {
                is Failure -> {
                    checkResult
                }

                is Success -> {
                    val (creatorInfo, channel) = checkResult.value
                    val token = UUID.randomUUID().toString()
                    val invitation =
                        repoInvitations.create(token, creatorInfo, channel, accessType, expiresAt)
                    success(invitation)
                }
            }
        }
    }

    fun getInvitationsForChannel(
        requesterId: Long,
        channelId: Long,
    ): Either<InvitationError, List<Invitation>> =
        trxManager.run {
            when (val checkResult = checkUserCanManageInvitations(requesterId, channelId)) {
                is Failure -> {
                    checkResult
                }

                is Success -> {
                    val invitations = repoInvitations.findByChannelId(channelId)
                    success(invitations)
                }
            }
        }

    fun revokeInvitation(
        userId: Long,
        channelId: Long,
        invitationId: Long,
    ): Either<InvitationError, String> =
        trxManager.run {
            val user =
                repoUsers.findById(userId) ?: return@run failure(InvitationError.UserNotFound)
            val channel =
                repoChannels.findById(channelId)
                    ?: return@run failure(InvitationError.ChannelNotFound)

            if (user.id != channel.owner.id) {
                return@run failure(InvitationError.UserNotAuthorized)
            }

            val invitation =
                repoInvitations.findById(invitationId)
                    ?: return@run failure(InvitationError.InvitationNotFound)

            repoInvitations.save(invitation.copy(status = InvitationStatus.REJECTED))
            success("Invitation revoked.")
        }

    private fun Transaction.checkUserCanManageInvitations(
        creatorId: Long,
        channelId: Long,
    ): Either<InvitationError, Pair<UserInfo, Channel>> {
        val creator = repoUsers.findById(creatorId) ?: return failure(InvitationError.UserNotFound)
        val channel =
            repoChannels.findById(channelId) ?: return failure(InvitationError.ChannelNotFound)
        val membership =
            repoMemberships.findUserInChannel(creator.id, channel.id)
                ?: return failure(InvitationError.UserNotInChannel)

        if (membership.accessType != AccessType.READ_WRITE) {
            return failure(InvitationError.UserNotAuthorized)
        }

        val creatorInfo = UserInfo(creator.id, creator.username)
        return success(creatorInfo to channel)
    }
}
