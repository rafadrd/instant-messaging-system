package pt.isel.repositories.messages

import pt.isel.domain.channels.Channel
import pt.isel.domain.messages.Message
import pt.isel.domain.users.UserInfo
import pt.isel.repositories.Repository

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
