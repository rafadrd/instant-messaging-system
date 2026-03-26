package pt.isel.infrastructure.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisTicketServiceTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisTicketService ticketService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ticketService = new RedisTicketService(redisTemplate);
    }

    @Test
    void testCreateTicket() {
        Long userId = 42L;
        String ticket = ticketService.createTicket(userId);

        assertNotNull(ticket);
        verify(valueOperations).set(eq("ticket:" + ticket), eq("42"), any(Duration.class));
    }

    @Test
    void testValidateAndConsumeTicketSuccess() {
        String ticket = "some-uuid-ticket";
        when(valueOperations.getAndDelete("ticket:" + ticket)).thenReturn("42");

        Long userId = ticketService.validateAndConsumeTicket(ticket);

        assertEquals(42L, userId);
        verify(valueOperations).getAndDelete("ticket:" + ticket);
    }

    @Test
    void testValidateAndConsumeTicketNotFound() {
        String ticket = "invalid-ticket";
        when(valueOperations.getAndDelete("ticket:" + ticket)).thenReturn(null);

        Long userId = ticketService.validateAndConsumeTicket(ticket);

        assertNull(userId);
    }

    @Test
    void testValidateAndConsumeTicketInvalidFormat() {
        String ticket = "corrupted-ticket";
        when(valueOperations.getAndDelete("ticket:" + ticket)).thenReturn("not-a-number");

        Long userId = ticketService.validateAndConsumeTicket(ticket);

        assertNull(userId);
    }
}