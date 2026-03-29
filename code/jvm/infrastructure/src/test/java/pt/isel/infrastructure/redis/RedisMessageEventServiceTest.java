package pt.isel.infrastructure.redis;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import pt.isel.domain.builders.MessageBuilder;
import pt.isel.domain.builders.UserInfoBuilder;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.messages.Message;
import pt.isel.domain.messages.UpdatedMessage;
import pt.isel.domain.messages.UpdatedMessageEmitter;
import pt.isel.domain.users.User;
import pt.isel.repositories.TransactionManager;
import pt.isel.repositories.contracts.RepositoryTestHelper;
import pt.isel.repositories.mem.TransactionManagerInMem;

import java.time.Clock;
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

@ExtendWith(MockitoExtension.class)
class RedisMessageEventServiceTest implements RepositoryTestHelper {

    private TransactionManagerInMem trxManager;

    @Mock
    private StringRedisTemplate redisTemplate;

    private ObjectMapper objectMapper;
    private RedisMessageEventService service;

    private User alice;
    private Channel channel;

    @Override
    public TransactionManager getTxManager() {
        return trxManager;
    }

    @BeforeEach
    void setUp() throws Exception {
        trxManager = new TransactionManagerInMem();

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.addMixIn(UpdatedMessage.class, UpdatedMessageMixin.class);

        service = new RedisMessageEventService(trxManager, redisTemplate, objectMapper, Clock.systemUTC());

        alice = trxManager.run(trx -> insertUser(trx, "alice"));
        channel = trxManager.run(trx -> {
            Channel c = insertChannel(trx, "General", alice, true);
            insertMember(trx, alice, c, AccessType.READ_WRITE);
            return c;
        });
    }

    @Test
    void AddEmitter_ValidInput_AddsEmitter() {
        UpdatedMessageEmitter emitter = mock(UpdatedMessageEmitter.class);

        assertThatCode(() -> service.addEmitter(channel.id(), alice.id(), emitter))
                .doesNotThrowAnyException();

        service.sendKeepAlive();
        verify(emitter).emit(any(UpdatedMessage.KeepAlive.class));
    }

    @Test
    void AddEmitter_UserNotFound_ThrowsException() {
        UpdatedMessageEmitter emitter = mock(UpdatedMessageEmitter.class);
        assertThatThrownBy(() -> service.addEmitter(channel.id(), 999L, emitter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void AddEmitter_ChannelNotFound_ThrowsException() {
        UpdatedMessageEmitter emitter = mock(UpdatedMessageEmitter.class);
        assertThatThrownBy(() -> service.addEmitter(999L, alice.id(), emitter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void AddEmitter_UserNotInChannel_ThrowsException() {
        User bob = trxManager.run(trx -> insertUser(trx, "bob"));
        UpdatedMessageEmitter emitter = mock(UpdatedMessageEmitter.class);

        assertThatThrownBy(() -> service.addEmitter(channel.id(), bob.id(), emitter))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void BroadcastMessage_ValidInput_SendsToRedis() {
        Message msg = new MessageBuilder()
                .withId(1L)
                .withContent("Hello")
                .withUser(new UserInfoBuilder().withId(alice.id()).withUsername(alice.username()).build())
                .withChannel(channel)
                .build();
        UpdatedMessage.NewMessage signal = new UpdatedMessage.NewMessage(msg);

        service.broadcastMessage(channel.id(), signal);

        verify(redisTemplate).convertAndSend(eq("chat-events"), anyString());
    }

    @Test
    void HandleRedisMessage_ValidMessage_EmitsToListeners() throws Exception {
        UpdatedMessageEmitter emitter = mock(UpdatedMessageEmitter.class);
        service.addEmitter(channel.id(), alice.id(), emitter);

        UpdatedMessage.KeepAlive keepAlive = new UpdatedMessage.KeepAlive(Instant.now());
        RedisMessageEventService.DistributedEvent event = new RedisMessageEventService.DistributedEvent(channel.id(), keepAlive);

        String json = objectMapper.writeValueAsString(event);

        service.handleRedisMessage(json);

        verify(emitter).emit(any(UpdatedMessage.KeepAlive.class));
    }

    @Test
    void Shutdown_ActiveEmitters_CompletesEmitters() {
        UpdatedMessageEmitter emitter = mock(UpdatedMessageEmitter.class);
        service.addEmitter(channel.id(), alice.id(), emitter);

        service.shutdown();

        verify(emitter).complete();

        service.sendKeepAlive();
        verify(emitter, never()).emit(any(UpdatedMessage.KeepAlive.class));
    }

    @Test
    void SendKeepAlive_EmitThrowsException_RemovesEmitter() {
        UpdatedMessageEmitter emitter = mock(UpdatedMessageEmitter.class);
        Mockito.doThrow(new RuntimeException("Connection closed")).when(emitter).emit(any());

        service.addEmitter(channel.id(), alice.id(), emitter);

        service.sendKeepAlive();

        service.sendKeepAlive();
        verify(emitter, Mockito.times(1)).emit(any());
    }

    @Test
    void HandleRedisMessage_EmitThrowsException_RemovesEmitter() throws Exception {
        UpdatedMessageEmitter emitter = mock(UpdatedMessageEmitter.class);
        Mockito.doThrow(new RuntimeException("Connection closed")).when(emitter).emit(any());

        service.addEmitter(channel.id(), alice.id(), emitter);

        UpdatedMessage.KeepAlive keepAlive = new UpdatedMessage.KeepAlive(Instant.now());
        RedisMessageEventService.DistributedEvent event = new RedisMessageEventService.DistributedEvent(channel.id(), keepAlive);

        String json = objectMapper.writeValueAsString(event);

        service.handleRedisMessage(json);
        service.handleRedisMessage(json);

        verify(emitter, Mockito.times(1)).emit(any());
    }

    @Test
    void OnCompletion_CallbackTriggered_RemovesEmitter() {
        UpdatedMessageEmitter emitter = mock(UpdatedMessageEmitter.class);
        ArgumentCaptor<Runnable> completionCaptor = ArgumentCaptor.forClass(Runnable.class);

        service.addEmitter(channel.id(), alice.id(), emitter);
        verify(emitter).onCompletion(completionCaptor.capture());

        completionCaptor.getValue().run();

        service.sendKeepAlive();
        verify(emitter, never()).emit(any());
    }

    @Test
    void HandleRedisMessage_InvalidJson_CatchesException() {
        assertThatCode(() -> service.handleRedisMessage("invalid-json-string"))
                .doesNotThrowAnyException();
    }

    @Test
    @SuppressWarnings("unchecked")
    void OnError_CallbackTriggered_RemovesEmitter() {
        UpdatedMessageEmitter emitter = mock(UpdatedMessageEmitter.class);
        ArgumentCaptor<Consumer<Throwable>> errorCaptor = ArgumentCaptor.forClass(Consumer.class);

        service.addEmitter(channel.id(), alice.id(), emitter);
        verify(emitter).onError(errorCaptor.capture());

        errorCaptor.getValue().accept(new RuntimeException("Client disconnected abruptly"));

        service.sendKeepAlive();
        verify(emitter, never()).emit(any());
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = UpdatedMessage.NewMessage.class, name = "new-message"),
            @JsonSubTypes.Type(value = UpdatedMessage.KeepAlive.class, name = "keep-alive")
    })
    abstract static class UpdatedMessageMixin {
    }
}