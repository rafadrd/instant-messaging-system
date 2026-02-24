package pt.isel

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
