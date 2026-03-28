package pt.isel.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pt.isel.domain.messages.UpdatedMessage;
import pt.isel.domain.messages.UpdatedMessageEmitter;
import pt.isel.repositories.TransactionManager;
import pt.isel.services.messages.MessageEventService;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RedisMessageEventService implements MessageEventService {
    private static final Logger logger = LoggerFactory.getLogger(RedisMessageEventService.class);
    private static final String TOPIC_NAME = "chat-events";

    private final TransactionManager trxManager;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Map<Long, Set<UpdatedMessageEmitter>> localListeners = new ConcurrentHashMap<>();

    public RedisMessageEventService(TransactionManager trxManager, StringRedisTemplate redisTemplate, ObjectMapper objectMapper, Clock clock) {
        this.trxManager = trxManager;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public void addEmitter(Long channelId, Long userId, UpdatedMessageEmitter emitter) {
        trxManager.run(trx -> {
            var user = trx.repoUsers().findById(userId);
            if (user == null) throw new IllegalArgumentException("User with ID " + userId + " not found.");

            var channel = trx.repoChannels().findById(channelId);
            if (channel == null) throw new IllegalArgumentException("Channel with ID " + channelId + " not found.");

            if (trx.repoMemberships().findUserInChannel(user.id(), channel.id()) == null) {
                throw new SecurityException("User " + userId + " is not a member of channel " + channelId + ".");
            }
            return null;
        });

        localListeners.computeIfAbsent(channelId, k -> ConcurrentHashMap.newKeySet()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(channelId, emitter));
        emitter.onError(e -> removeEmitter(channelId, emitter));
    }

    private void removeEmitter(Long channelId, UpdatedMessageEmitter emitter) {
        localListeners.computeIfPresent(channelId, (id, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }

    @Override
    public void broadcastMessage(Long channelId, UpdatedMessage signal) {
        try {
            DistributedEvent event = new DistributedEvent(channelId, signal);
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(TOPIC_NAME, json);
        } catch (Exception e) {
            logger.error("Failed to publish message to Redis", e);
        }
    }

    public void handleRedisMessage(String json) {
        try {
            DistributedEvent event = objectMapper.readValue(json, DistributedEvent.class);
            Set<UpdatedMessageEmitter> emitters = localListeners.get(event.channelId());
            if (emitters != null) {
                for (UpdatedMessageEmitter emitter : emitters) {
                    try {
                        emitter.emit(event.message());
                    } catch (Exception e) {
                        removeEmitter(event.channelId(), emitter);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to process Redis message", e);
        }
    }

    @Scheduled(fixedRate = 30000)
    public void sendKeepAlive() {
        Instant now = Instant.now(clock);
        UpdatedMessage.KeepAlive keepAlive = new UpdatedMessage.KeepAlive(now);

        localListeners.forEach((channelId, emitters) -> {
            for (UpdatedMessageEmitter emitter : emitters) {
                try {
                    emitter.emit(keepAlive);
                } catch (Exception e) {
                    removeEmitter(channelId, emitter);
                }
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Completing all SSE streams before shutdown...");
        localListeners.forEach((channelId, emitters) -> {
            for (UpdatedMessageEmitter emitter : emitters) {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    logger.warn("Failed to complete emitter during shutdown", e);
                }
            }
        });
        localListeners.clear();
    }

    public record DistributedEvent(Long channelId, UpdatedMessage message) {
    }
}