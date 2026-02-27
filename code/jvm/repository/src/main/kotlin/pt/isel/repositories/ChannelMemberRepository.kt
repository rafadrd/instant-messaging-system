package pt.isel.repositories

import pt.isel.domain.channel.AccessType
import pt.isel.domain.channel.Channel
import pt.isel.domain.channel.ChannelMember
import pt.isel.domain.user.UserInfo

/** Repository interface for managing channel members, extends the generic Repository */
interface ChannelMemberRepository : Repository<ChannelMember> {
    fun addUserToChannel(
        userInfo: UserInfo,
        channel: Channel,
        accessType: AccessType,
    ): ChannelMember

    fun findUserInChannel(
        userId: Long,
        channelId: Long,
    ): ChannelMember?

    fun findAllChannelsForUser(
        userId: Long,
        limit: Int = 50,
        offset: Int = 0,
    ): List<ChannelMember>

    fun removeUserFromChannel(
        userId: Long,
        channelId: Long,
    )

    fun findAllMembersInChannel(
        channelId: Long,
        limit: Int = 50,
        offset: Int = 0,
    ): List<ChannelMember>
}
