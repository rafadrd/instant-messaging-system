package pt.isel.domain.invitation

import pt.isel.domain.channel.AccessType
import pt.isel.domain.channel.Channel
import pt.isel.domain.user.UserInfo
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
