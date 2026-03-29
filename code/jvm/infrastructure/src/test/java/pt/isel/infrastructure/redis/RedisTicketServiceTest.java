package pt.isel.infrastructure.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisTicketServiceTest {

    private ValueOperations<String, String> valueOperations;
    private RedisTicketService ticketService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ticketService = new RedisTicketService(redisTemplate);
    }

    @Test
    void CreateTicket_ValidUserId_ReturnsTicket() {
        Long userId = 42L;

        String ticket = ticketService.createTicket(userId);

        assertThat(ticket).isNotNull();
        verify(valueOperations).set(eq("ticket:" + ticket), eq("42"), any(Duration.class));
    }

    @Test
    void ValidateAndConsumeTicket_ValidTicket_ReturnsUserId() {
        String ticket = "some-uuid-ticket";
        when(valueOperations.getAndDelete("ticket:" + ticket)).thenReturn("42");

        Long userId = ticketService.validateAndConsumeTicket(ticket);

        assertThat(userId).isEqualTo(42L);
        verify(valueOperations).getAndDelete("ticket:" + ticket);
    }

    @Test
    void ValidateAndConsumeTicket_InvalidTicket_ReturnsNull() {
        String ticket = "invalid-ticket";
        when(valueOperations.getAndDelete("ticket:" + ticket)).thenReturn(null);

        Long userId = ticketService.validateAndConsumeTicket(ticket);

        assertThat(userId).isNull();
    }

    @Test
    void ValidateAndConsumeTicket_InvalidFormat_ReturnsNull() {
        String ticket = "corrupted-ticket";
        when(valueOperations.getAndDelete("ticket:" + ticket)).thenReturn("not-a-number");

        Long userId = ticketService.validateAndConsumeTicket(ticket);

        assertThat(userId).isNull();
    }
}