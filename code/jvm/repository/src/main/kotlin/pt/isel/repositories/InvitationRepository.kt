package pt.isel.repositories

import pt.isel.domain.channel.AccessType
import pt.isel.domain.channel.Channel
import pt.isel.domain.invitation.Invitation
import pt.isel.domain.user.UserInfo
import java.time.LocalDateTime

/** Repository interface for managing invitations, extends the generic Repository */
interface InvitationRepository : Repository<Invitation> {
    fun create(
        token: String,
        createdBy: UserInfo,
        channel: Channel,
        accessType: AccessType,
        expiresAt: LocalDateTime,
    ): Invitation

    fun findByToken(token: String): Invitation?

    fun findByChannelId(channelId: Long): List<Invitation>
}
