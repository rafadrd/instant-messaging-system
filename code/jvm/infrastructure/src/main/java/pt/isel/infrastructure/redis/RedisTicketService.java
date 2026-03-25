package pt.isel.infrastructure.redis;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import pt.isel.services.users.TicketService;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Service
public class RedisTicketService implements TicketService {
    private static final Logger logger = LoggerFactory.getLogger(RedisTicketService.class);
    private static final String PREFIX = "ticket:";
    private static final Duration TICKET_TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redisTemplate;

    public RedisTicketService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String createTicket(Long userId) {
        String ticket = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(PREFIX + ticket, userId.toString(), TICKET_TTL);
        return ticket;
    }

    @Override
    public Long validateAndConsumeTicket(String ticket) {
        String key = PREFIX + ticket;
        String userIdStr = redisTemplate.opsForValue().getAndDelete(key);

        if (userIdStr == null) return null;

        try {
            return Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @PreDestroy
    public void flushTickets() {
        logger.info("Flushing Redis tickets before shutdown...");
        try {
            Set<String> keys = redisTemplate.keys(PREFIX + "*");
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.info("Flushed {} tickets.", keys.size());
            }
        } catch (Exception e) {
            logger.error("Failed to flush Redis tickets during shutdown", e);
        }
    }
}