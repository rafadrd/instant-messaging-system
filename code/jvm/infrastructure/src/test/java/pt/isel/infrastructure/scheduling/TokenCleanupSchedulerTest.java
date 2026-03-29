package pt.isel.infrastructure.scheduling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import pt.isel.services.users.UserService;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenCleanupSchedulerTest {

    @Mock
    private UserService userService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void RunCleanup_LockAcquired_ExecutesCleanup() {
        when(valueOperations.setIfAbsent(eq("scheduler:token-cleanup:lock"), eq("locked"), any(Duration.class))).thenReturn(true);

        scheduler.runCleanup();

        verify(userService).cleanupExpiredTokens();
    }

    @Test
    void RunCleanup_LockNotAcquired_SkipsCleanup() {
        when(valueOperations.setIfAbsent(eq("scheduler:token-cleanup:lock"), eq("locked"), any(Duration.class))).thenReturn(false);

        scheduler.runCleanup();

        verify(userService, never()).cleanupExpiredTokens();
    }

    @Test
    void RunCleanup_ServiceThrowsException_HandlesGracefully() {
        when(valueOperations.setIfAbsent(eq("scheduler:token-cleanup:lock"), eq("locked"), any(Duration.class))).thenReturn(true);
        doThrow(new RuntimeException("Database connection failed")).when(userService).cleanupExpiredTokens();

        assertThatCode(() -> scheduler.runCleanup()).doesNotThrowAnyException();

        verify(userService).cleanupExpiredTokens();
    }
}