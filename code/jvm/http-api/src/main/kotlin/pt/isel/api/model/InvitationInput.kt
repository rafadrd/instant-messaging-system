package pt.isel.api.model

import pt.isel.domain.channel.AccessType
import java.time.LocalDateTime

data class InvitationInput(
    val accessType: AccessType,
    val expiresAt: LocalDateTime,
)
