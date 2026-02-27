package pt.isel.domain.channel

import pt.isel.domain.user.UserInfo

data class ChannelMember(
    val id: Long,
    val user: UserInfo,
    val channel: Channel,
    val accessType: AccessType,
)
