package pt.isel.infrastructure.scheduling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import pt.isel.services.users.UserService;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenCleanupSchedulerTest {

    private UserService userService;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private TokenCleanupScheduler scheduler;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        userService = mock(UserService.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        scheduler = new TokenCleanupScheduler(userService, redisTemplate);
    }

    @Test
    void testRunCleanupExecutesWhenLockAcquired() {
        when(valueOperations.setIfAbsent(eq("scheduler:token-cleanup:lock"), eq("locked"), any(Duration.class))).thenReturn(true);

        scheduler.runCleanup();

        verify(userService).cleanupExpiredTokens();
    }

    @Test
    void testRunCleanupSkipsWhenLockNotAcquired() {
        when(valueOperations.setIfAbsent(eq("scheduler:token-cleanup:lock"), eq("locked"), any(Duration.class))).thenReturn(false);

        scheduler.runCleanup();

        verify(userService, never()).cleanupExpiredTokens();
    }

    @Test
    void testRunCleanupHandlesExceptionGracefully() {
        when(valueOperations.setIfAbsent(eq("scheduler:token-cleanup:lock"), eq("locked"), any(Duration.class)))
                .thenReturn(true);

        doThrow(new RuntimeException("Database connection failed")).when(userService).cleanupExpiredTokens();

        assertDoesNotThrow(() -> scheduler.runCleanup());

        verify(userService).cleanupExpiredTokens();
    }
}