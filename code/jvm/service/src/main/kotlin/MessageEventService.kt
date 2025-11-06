package pt.isel

import jakarta.inject.Named
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

@Named
class MessageEventService(
    private val trxManager: TransactionManager,
    private val sseExecutor: Executor,
) {
    private val listeners = ConcurrentHashMap<Long, MutableSet<UpdatedMessageEmitter>>()

    fun addEmitter(
        channelId: Long,
        userId: Long,
        emitter: UpdatedMessageEmitter,
    ) {
        trxManager.run {
            val user =
                repoUsers.findById(userId)
                    ?: throw IllegalArgumentException("User with ID $userId not found.")
            val channel =
                repoChannels.findById(channelId)
                    ?: throw IllegalArgumentException("Channel with ID $channelId not found.")
            repoMemberships.findUserInChannel(user.id, channel.id)
                ?: throw SecurityException("User $userId is not a member of channel $channelId.")
        }

        listeners
            .computeIfAbsent(channelId) { ConcurrentHashMap.newKeySet() }
            .apply {
                add(emitter)
                logger.debug("Emitter added to channel $channelId. Total emitters: $size")
            }

        emitter.onCompletion { removeEmitter(channelId, emitter) }
        emitter.onError { removeEmitter(channelId, emitter) }
    }

    private fun removeEmitter(
        channelId: Long,
        emitter: UpdatedMessageEmitter,
    ) {
        listeners[channelId]?.run {
            if (remove(emitter)) {
                logger.debug("Emitter removed from channel $channelId. Remaining emitters: $size")
                if (isEmpty()) listeners.remove(channelId)
            }
        }
    }

    fun broadcastMessage(
        channelId: Long,
        signal: UpdatedMessage,
    ) {
        listeners[channelId]?.forEach { emitter ->
            sseExecutor.execute {
                try {
                    emitter.emit(signal)
                } catch (e: Exception) {
                    logger.error(
                        "Error broadcasting message to channel $channelId: ${e.message}",
                        e,
                    )
                    removeEmitter(channelId, emitter)
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MessageEventService::class.java)
    }
}
