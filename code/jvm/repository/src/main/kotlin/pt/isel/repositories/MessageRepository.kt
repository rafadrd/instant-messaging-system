package pt.isel.repositories

import pt.isel.domain.Channel
import pt.isel.domain.Message
import pt.isel.domain.UserInfo

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
