package pt.isel.services

import pt.isel.domain.message.UpdatedMessage
import pt.isel.domain.message.UpdatedMessageEmitter

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
