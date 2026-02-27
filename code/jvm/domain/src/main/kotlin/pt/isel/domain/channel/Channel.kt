package pt.isel.domain.channel

import pt.isel.domain.user.UserInfo

data class Channel(
    val id: Long,
    val name: String,
    val owner: UserInfo,
    val isPublic: Boolean = true,
)
