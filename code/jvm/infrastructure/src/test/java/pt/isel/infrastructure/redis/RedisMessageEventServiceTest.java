package pt.isel.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.messages.Message;
import pt.isel.domain.messages.UpdatedMessage;
import pt.isel.domain.messages.UpdatedMessageEmitter;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.User;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.mem.TransactionManagerInMem;

import java.time.Instant;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisMessageEventServiceTest {

    private TransactionManagerInMem trxManager;
    private StringRedisTemplate redisTemplate;
    private ObjectMapper objectMapper;
    private RedisMessageEventService service;

    private User alice;
    private Channel channel;

    @BeforeEach
    void setUp() throws Exception {
        trxManager = new TransactionManagerInMem();
        redisTemplate = mock(StringRedisTemplate.class);

        objectMapper = mock(ObjectMapper.class);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"dummy\":\"json\"}");

        service = new RedisMessageEventService(trxManager, redisTemplate, objectMapper);

        alice = trxManager.run(trx -> trx.repoUsers().create("alice", new PasswordValidationInfo("hash")));
        channel = trxManager.run(trx -> {
            UserInfo aliceInfo = new UserInfo(alice.id(), alice.username());
            Channel c = trx.repoChannels().create("General", aliceInfo, true);
            trx.repoMemberships().addUserToChannel(aliceInfo, c, AccessType.READ_WRITE);
            return c;
        });
    }

    @Test
    void testAddEmitterSuccess() {
        UpdatedMessageEmitter emitter = mock(UpdatedMessageEmitter.class);

        assertThatCode(() -> service.addEmitter(channel.id(), alice.id(), emitter))
                .doesNotThrowAnyException();

        service.sendKeepAlive();
        verify(emitter).emit(any(UpdatedMessage.KeepAlive.class));
    }

    @Test
    void testAddEmitterThrowsWhenUserNotFound() {
        UpdatedMessageEmitter emitter = mock(UpdatedMessageEmitter.class);
        assertThatThrownBy(() -> service.addEmitter(channel.id(), 999L, emitter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void testAddEmitterThrowsWhenChannelNotFound() {
        UpdatedMessageEmitter emitter = mock(UpdatedMessageEmitter.class);
        assertThatThrownBy(() -> service.addEmitter(999L, alice.id(), emitter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void testAddEmitterThrowsWhenUserNotInChannel() {
        User bob = trxManager.run(trx -> trx.repoUsers().create("bob", new PasswordValidationInfo("hash")));
        UpdatedMessageEmitter emitter = mock(UpdatedMessageEmitter.class);

        assertThatThrownBy(() -> service.addEmitter(channel.id(), bob.id(), emitter))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void testBroadcastMessage() {
        Message msg = new Message(1L, "Hello", new UserInfo(alice.id(), alice.username()), channel);
        UpdatedMessage.NewMessage signal = new UpdatedMessage.NewMessage(msg);

        service.broadcastMessage(channel.id(), signal);

        verify(redisTemplate).convertAndSend(eq("chat-events"), anyString());
    }

    @Test
    void testHandleRedisMessageEmitsToLocalListeners() throws Exception {
        UpdatedMessageEmitter emitter = mock(UpdatedMessageEmitter.class);
        service.addEmitter(channel.id(), alice.id(), emitter);

        UpdatedMessage.KeepAlive keepAlive = new UpdatedMessage.KeepAlive(Instant.now());
        RedisMessageEventService.DistributedEvent event = new RedisMessageEventService.DistributedEvent(channel.id(), keepAlive);

        when(objectMapper.readValue(anyString(), eq(RedisMessageEventService.DistributedEvent.class)))
                .thenReturn(event);

        service.handleRedisMessage("{\"dummy\":\"json\"}");

        verify(emitter).emit(any(UpdatedMessage.KeepAlive.class));
    }

    @Test
    void testShutdownCompletesAllEmitters() {
        UpdatedMessageEmitter emitter = mock(UpdatedMessageEmitter.class);
        service.addEmitter(channel.id(), alice.id(), emitter);

        service.shutdown();

        verify(emitter).complete();

        service.sendKeepAlive();
        verify(emitter, never()).emit(any(UpdatedMessage.KeepAlive.class));
    }

    @Test
    void testEmitterRemovedOnEmitExceptionDuringKeepAlive() {
        UpdatedMessageEmitter emitter = mock(UpdatedMessageEmitter.class);
        Mockito.doThrow(new RuntimeException("Connection closed")).when(emitter).emit(any());

        service.addEmitter(channel.id(), alice.id(), emitter);

        service.sendKeepAlive();

        service.sendKeepAlive();
        verify(emitter, Mockito.times(1)).emit(any());
    }

    @Test
    void testEmitterRemovedOnEmitExceptionDuringHandleRedisMessage() throws Exception {
        UpdatedMessageEmitter emitter = mock(UpdatedMessageEmitter.class);
        Mockito.doThrow(new RuntimeException("Connection closed")).when(emitter).emit(any());

        service.addEmitter(channel.id(), alice.id(), emitter);

        UpdatedMessage.KeepAlive keepAlive = new UpdatedMessage.KeepAlive(Instant.now());
        RedisMessageEventService.DistributedEvent event = new RedisMessageEventService.DistributedEvent(channel.id(), keepAlive);

        when(objectMapper.readValue(anyString(), eq(RedisMessageEventService.DistributedEvent.class)))
                .thenReturn(event);

        service.handleRedisMessage("{\"dummy\":\"json\"}");
        service.handleRedisMessage("{\"dummy\":\"json\"}");

        verify(emitter, Mockito.times(1)).emit(any());
    }

    @Test
    void testCallbacksRemoveEmitter() {
        UpdatedMessageEmitter emitter = mock(UpdatedMessageEmitter.class);
        ArgumentCaptor<Runnable> completionCaptor = ArgumentCaptor.forClass(Runnable.class);

        service.addEmitter(channel.id(), alice.id(), emitter);
        verify(emitter).onCompletion(completionCaptor.capture());

        completionCaptor.getValue().run();

        service.sendKeepAlive();
        verify(emitter, never()).emit(any());
    }

    @Test
    void testBroadcastMessageCatchesJsonProcessingException() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Serialization error") {
        });

        UpdatedMessage.KeepAlive keepAlive = new UpdatedMessage.KeepAlive(Instant.now());

        assertThatCode(() -> service.broadcastMessage(channel.id(), keepAlive))
                .doesNotThrowAnyException();
        verify(redisTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    void testHandleRedisMessageCatchesJsonProcessingException() throws Exception {
        when(objectMapper.readValue(anyString(), eq(RedisMessageEventService.DistributedEvent.class)))
                .thenThrow(new JsonProcessingException("Deserialization error") {
                });

        assertThatCode(() -> service.handleRedisMessage("invalid-json-string"))
                .doesNotThrowAnyException();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testOnErrorCallbackRemovesEmitter() {
        UpdatedMessageEmitter emitter = mock(UpdatedMessageEmitter.class);
        ArgumentCaptor<Consumer<Throwable>> errorCaptor = ArgumentCaptor.forClass(Consumer.class);

        service.addEmitter(channel.id(), alice.id(), emitter);
        verify(emitter).onError(errorCaptor.capture());

        errorCaptor.getValue().accept(new RuntimeException("Client disconnected abruptly"));

        service.sendKeepAlive();
        verify(emitter, never()).emit(any());
    }
}