package pt.isel.api.channels

import pt.isel.domain.channels.AccessType

data class EditMemberInput(
    val accessType: AccessType,
)
