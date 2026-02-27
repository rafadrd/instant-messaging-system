package pt.isel.repositories.invitations

import pt.isel.domain.channels.AccessType
import pt.isel.domain.channels.Channel
import pt.isel.domain.invitations.Invitation
import pt.isel.domain.users.UserInfo
import pt.isel.repositories.Repository
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
