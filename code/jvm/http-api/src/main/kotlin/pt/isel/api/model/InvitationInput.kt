package pt.isel.api.model

import pt.isel.domain.AccessType
import java.time.LocalDateTime

data class InvitationInput(
    val accessType: AccessType,
    val expiresAt: LocalDateTime,
)
