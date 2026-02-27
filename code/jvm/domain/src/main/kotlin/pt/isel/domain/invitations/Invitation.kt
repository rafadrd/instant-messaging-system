package pt.isel.domain.invitations

import pt.isel.domain.channels.AccessType
import pt.isel.domain.channels.Channel
import pt.isel.domain.users.UserInfo
import java.time.LocalDateTime

data class Invitation(
    val id: Long,
    val token: String,
    val createdBy: UserInfo,
    val channel: Channel,
    val accessType: AccessType,
    val expiresAt: LocalDateTime,
    val status: InvitationStatus = InvitationStatus.PENDING,
)
