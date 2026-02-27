package pt.isel.repositories.mem

import jakarta.inject.Named
import pt.isel.domain.channels.Channel
import pt.isel.domain.messages.Message
import pt.isel.domain.users.UserInfo
import pt.isel.repositories.messages.MessageRepository

/**
 * Naif in memory repository non thread-safe and basic sequential id. Useful for unit tests purpose.
 */
@Named
class MessageRepositoryInMem : MessageRepository {
    private val messages = mutableListOf<Message>()

    override fun create(
        content: String,
        user: UserInfo,
        channel: Channel,
    ): Message =
        Message(
            messages.size.toLong() + 1,
            content,
            user,
            channel,
        ).also { messages.add(it) }

    override fun findById(id: Long): Message? = messages.firstOrNull { it.id == id }

    override fun findAllInChannel(
        channel: Channel,
        limit: Int,
        offset: Int,
    ): List<Message> = messages.filter { it.channel == channel }.drop(offset).take(limit)

    override fun findAll(): List<Message> = messages.toList()

    override fun save(entity: Message) {
        messages.removeIf { it.id == entity.id }.apply { messages.add(entity) }
    }

    override fun deleteById(id: Long) {
        messages.removeIf { it.id == id }
    }

    override fun clear() {
        messages.clear()
    }
}
