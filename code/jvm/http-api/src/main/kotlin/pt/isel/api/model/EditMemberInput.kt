package pt.isel.api.model

import pt.isel.domain.AccessType

data class EditMemberInput(
    val accessType: AccessType,
)
