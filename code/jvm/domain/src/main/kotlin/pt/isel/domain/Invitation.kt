package pt.isel.domain

import java.time.LocalDateTime

data class Invitation(
    val id: Long,
    val token: String,
    val createdBy: UserInfo,
    val channel: Channel,
    val accessType: AccessType,
    val expiresAt: LocalDateTime,
    val status: Status = Status.PENDING,
)
