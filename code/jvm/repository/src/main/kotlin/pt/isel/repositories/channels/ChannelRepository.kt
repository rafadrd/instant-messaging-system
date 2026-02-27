package pt.isel.repositories.channels

import pt.isel.domain.channels.Channel
import pt.isel.domain.users.UserInfo
import pt.isel.repositories.Repository

/** Repository interface for managing channels, extends the generic Repository */
interface ChannelRepository : Repository<Channel> {
    fun create(
        name: String,
        owner: UserInfo,
        isPublic: Boolean,
    ): Channel

    fun findByName(name: String): Channel?

    fun findAllByOwner(ownerId: Long): List<Channel>

    fun findAllPublicChannels(
        limit: Int = 50,
        offset: Int = 0,
    ): List<Channel>

    fun searchByName(
        query: String,
        limit: Int = 50,
        offset: Int = 0,
    ): List<Channel>
}
