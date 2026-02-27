package pt.isel.domain.channels

import pt.isel.domain.users.UserInfo

data class ChannelMember(
    val id: Long,
    val user: UserInfo,
    val channel: Channel,
    val accessType: AccessType,
)
