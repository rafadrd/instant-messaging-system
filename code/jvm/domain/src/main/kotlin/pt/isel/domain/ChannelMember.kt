package pt.isel.domain

data class ChannelMember(
    val id: Long,
    val user: UserInfo,
    val channel: Channel,
    val accessType: AccessType,
)
