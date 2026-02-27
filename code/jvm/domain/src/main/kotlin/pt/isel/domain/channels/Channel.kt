package pt.isel.domain.channels

import pt.isel.domain.users.UserInfo

data class Channel(
    val id: Long,
    val name: String,
    val owner: UserInfo,
    val isPublic: Boolean = true,
)
