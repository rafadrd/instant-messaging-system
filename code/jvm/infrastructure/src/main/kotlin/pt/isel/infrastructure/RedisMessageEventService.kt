package pt.isel.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import pt.isel.domain.UpdatedMessage
import pt.isel.domain.UpdatedMessageEmitter
import pt.isel.repositories.TransactionManager
import pt.isel.services.MessageEventService
import java.util.concurrent.ConcurrentHashMap

@Service
class RedisMessageEventService(
    private val trxManager: TransactionManager,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : MessageEventService {
    private val localListeners = ConcurrentHashMap<Long, MutableSet<UpdatedMessageEmitter>>()

    data class DistributedEvent(
        val channelId: Long,
        val message: UpdatedMessage,
    )

    override fun addEmitter(
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

        localListeners
            .computeIfAbsent(channelId) { ConcurrentHashMap.newKeySet() }
            .add(emitter)

        emitter.onCompletion { removeEmitter(channelId, emitter) }
        emitter.onError { removeEmitter(channelId, emitter) }
    }

    private fun removeEmitter(
        channelId: Long,
        emitter: UpdatedMessageEmitter,
    ) {
        localListeners[channelId]?.apply {
            remove(emitter)
            if (isEmpty()) localListeners.remove(channelId)
        }
    }

    override fun broadcastMessage(
        channelId: Long,
        signal: UpdatedMessage,
    ) {
        try {
            val event = DistributedEvent(channelId, signal)
            val json = objectMapper.writeValueAsString(event)
            redisTemplate.convertAndSend(TOPIC_NAME, json)
        } catch (e: Exception) {
            logger.error("Failed to publish message to Redis", e)
        }
    }

    fun handleRedisMessage(json: String) {
        try {
            val event = objectMapper.readValue<DistributedEvent>(json)

            localListeners[event.channelId]?.forEach { emitter ->
                try {
                    emitter.emit(event.message)
                } catch (_: Exception) {
                    removeEmitter(event.channelId, emitter)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to process Redis message", e)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RedisMessageEventService::class.java)
        private const val TOPIC_NAME = "chat-events"
    }
}
