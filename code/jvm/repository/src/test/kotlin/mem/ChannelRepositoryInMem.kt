package pt.isel.mem

import jakarta.inject.Named
import pt.isel.Channel
import pt.isel.ChannelRepository
import pt.isel.UserInfo

/**
 * Naif in memory repository non thread-safe and basic sequential id. Useful for unit tests purpose.
 */
@Named
class ChannelRepositoryInMem : ChannelRepository {
    private val channels = mutableListOf<Channel>()

    override fun create(
        name: String,
        owner: UserInfo,
        isPublic: Boolean,
    ): Channel =
        Channel(
            channels.size.toLong() + 1,
            name,
            owner,
            isPublic,
        ).also { channels.add(it) }

    override fun findById(id: Long): Channel? = channels.firstOrNull { it.id == id }

    override fun findByName(name: String): Channel? = channels.firstOrNull { it.name == name }

    override fun findAllPublicChannels(
        limit: Int,
        offset: Int,
    ): List<Channel> = channels.filter { it.isPublic }.drop(offset).take(limit)

    override fun findAllByOwner(ownerId: Long): List<Channel> = channels.filter { it.owner.id == ownerId }

    override fun findAll(): List<Channel> = channels.toList()

    override fun searchByName(
        query: String,
        limit: Int,
        offset: Int,
    ): List<Channel> =
        channels
            .filter { it.isPublic && it.name.contains(query, ignoreCase = true) }
            .drop(offset)
            .take(limit)

    override fun save(entity: Channel) {
        channels.removeIf { it.id == entity.id }.apply { channels.add(entity) }
    }

    override fun deleteById(id: Long) {
        channels.removeIf { it.id == id }
    }

    override fun clear() {
        channels.clear()
    }
}
