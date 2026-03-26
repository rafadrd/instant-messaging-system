package pt.isel.infrastructure.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import pt.isel.services.users.TicketService;

import java.time.Duration;
import java.util.UUID;

@Service
public class RedisTicketService implements TicketService {
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
}