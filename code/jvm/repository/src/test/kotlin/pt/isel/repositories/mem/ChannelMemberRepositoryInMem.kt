package pt.isel.repositories.mem

import jakarta.inject.Named
import pt.isel.domain.channels.AccessType
import pt.isel.domain.channels.Channel
import pt.isel.domain.channels.ChannelMember
import pt.isel.domain.users.UserInfo
import pt.isel.repositories.channels.ChannelMemberRepository

/**
 * Naif in memory repository non thread-safe and basic sequential id. Useful for unit tests purpose.
 */
@Named
class ChannelMemberRepositoryInMem : ChannelMemberRepository {
    private val channelMembers = mutableListOf<ChannelMember>()

    override fun addUserToChannel(
        userInfo: UserInfo,
        channel: Channel,
        accessType: AccessType,
    ): ChannelMember =
        ChannelMember(
            channelMembers.size.toLong() + 1,
            userInfo,
            channel,
            accessType,
        ).also {
            channelMembers.add(it)
        }

    override fun findById(id: Long): ChannelMember? = channelMembers.firstOrNull { it.id == id }

    override fun findUserInChannel(
        userId: Long,
        channelId: Long,
    ): ChannelMember? = channelMembers.firstOrNull { it.user.id == userId && it.channel.id == channelId }

    override fun findAllChannelsForUser(
        userId: Long,
        limit: Int,
        offset: Int,
    ): List<ChannelMember> = channelMembers.filter { it.user.id == userId }.drop(offset).take(limit)

    override fun findAllMembersInChannel(
        channelId: Long,
        limit: Int,
        offset: Int,
    ): List<ChannelMember> = channelMembers.filter { it.channel.id == channelId }.drop(offset).take(limit)

    override fun findAll(): List<ChannelMember> = channelMembers.toList()

    override fun save(entity: ChannelMember) {
        channelMembers.removeIf { it.id == entity.id }.apply { channelMembers.add(entity) }
    }

    override fun removeUserFromChannel(
        userId: Long,
        channelId: Long,
    ) {
        channelMembers.removeIf { it.user.id == userId && it.channel.id == channelId }
    }

    override fun deleteById(id: Long) {
        channelMembers.removeIf { it.id == id }
    }

    override fun clear() {
        channelMembers.clear()
    }
}
