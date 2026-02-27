package pt.isel.repositories

import pt.isel.domain.channel.Channel
import pt.isel.domain.message.Message
import pt.isel.domain.user.UserInfo

/** Repository interface for managing messages, extends the generic Repository */
interface MessageRepository : Repository<Message> {
    fun create(
        content: String,
        user: UserInfo,
        channel: Channel,
    ): Message

    fun findAllInChannel(
        channel: Channel,
        limit: Int = 50,
        offset: Int = 0,
    ): List<Message>
}
