package pt.isel.services.messages

import pt.isel.domain.messages.UpdatedMessage
import pt.isel.domain.messages.UpdatedMessageEmitter

interface MessageEventService {
    fun addEmitter(
        channelId: Long,
        userId: Long,
        emitter: UpdatedMessageEmitter,
    )

    fun broadcastMessage(
        channelId: Long,
        signal: UpdatedMessage,
    )
}
