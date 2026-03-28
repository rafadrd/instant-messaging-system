package pt.isel.infrastructure.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import pt.isel.services.common.RateLimiter;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
public class RedisRateLimiter implements RateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> script;
    private final Clock clock;

    public RedisRateLimiter(StringRedisTemplate redisTemplate, Clock clock) {
        this.redisTemplate = redisTemplate;
        this.clock = clock;
        String lua = """
                local key = KEYS[1]
                local now = tonumber(ARGV[1])
                local window = tonumber(ARGV[2])
                local limit = tonumber(ARGV[3])
                local member = ARGV[4]
                
                local clearBefore = now - window
                redis.call('ZREMRANGEBYSCORE', key, 0, clearBefore)
                
                local count = redis.call('ZCARD', key)
                if count >= limit then
                    return 0
                else
                    redis.call('ZADD', key, now, member)
                    redis.call('PEXPIRE', key, window)
                    return 1
                end
                """;
        this.script = new DefaultRedisScript<>(lua, Long.class);
    }

    @Override
    public boolean isRateLimited(String action, String identifier, int limit, Duration window) {
        String key = "rate_limit:" + action + ":" + identifier;
        long now = clock.millis();
        long windowMillis = window.toMillis();

        String member = now + "-" + UUID.randomUUID();

        Long result = redisTemplate.execute(
                script,
                List.of(key),
                String.valueOf(now),
                String.valueOf(windowMillis),
                String.valueOf(limit),
                member
        );

        return result == 0L;
    }
}