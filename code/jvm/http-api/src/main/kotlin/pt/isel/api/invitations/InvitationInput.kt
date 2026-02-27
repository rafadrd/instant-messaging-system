package pt.isel.api.invitations

import pt.isel.domain.channels.AccessType
import java.time.LocalDateTime

data class InvitationInput(
    val accessType: AccessType,
    val expiresAt: LocalDateTime,
)
