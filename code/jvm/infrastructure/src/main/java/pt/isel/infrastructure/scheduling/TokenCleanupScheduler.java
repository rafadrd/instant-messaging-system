package pt.isel.infrastructure.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pt.isel.services.users.UserService;

import java.time.Duration;

@Component
public class TokenCleanupScheduler {
    private static final Logger logger = LoggerFactory.getLogger(TokenCleanupScheduler.class);
    private static final String LOCK_KEY = "scheduler:token-cleanup:lock";

    private final UserService userService;
    private final StringRedisTemplate redisTemplate;

    public TokenCleanupScheduler(UserService userService, StringRedisTemplate redisTemplate) {
        this.userService = userService;
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(fixedRate = 3600000) // 1 hour
    public void runCleanup() {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(LOCK_KEY, "locked", Duration.ofMinutes(5));

        if (Boolean.TRUE.equals(acquired)) {
            logger.info("Acquired lock. Running token cleanup.");
            try {
                userService.cleanupExpiredTokens();
            } catch (Exception e) {
                logger.error("Error during token cleanup", e);
            }
        } else {
            logger.debug("Could not acquire lock. Cleanup handled by another instance.");
        }
    }
}