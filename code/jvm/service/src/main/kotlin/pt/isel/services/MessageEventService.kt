package pt.isel.services

import pt.isel.domain.UpdatedMessage
import pt.isel.domain.UpdatedMessageEmitter

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
