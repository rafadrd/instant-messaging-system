package pt.isel.api.model

import pt.isel.domain.channel.AccessType

data class EditMemberInput(
    val accessType: AccessType,
)
