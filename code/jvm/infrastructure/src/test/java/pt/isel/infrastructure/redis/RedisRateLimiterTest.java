package pt.isel.infrastructure.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private RedisRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneOffset.UTC);
        rateLimiter = new RedisRateLimiter(redisTemplate, clock);
    }

    @Test
    @SuppressWarnings("unchecked")
    void IsRateLimited_LimitExceeded_ReturnsTrue() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString(), anyString(), anyString())).thenReturn(0L);

        boolean result = rateLimiter.isRateLimited("login", "user123", 5, Duration.ofMinutes(1));

        assertThat(result).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void IsRateLimited_UnderLimit_ReturnsFalse() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString(), anyString(), anyString())).thenReturn(1L);

        boolean result = rateLimiter.isRateLimited("login", "user123", 5, Duration.ofMinutes(1));

        assertThat(result).isFalse();
    }
}