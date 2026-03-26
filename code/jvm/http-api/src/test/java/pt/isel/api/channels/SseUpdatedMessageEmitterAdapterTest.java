package pt.isel.api.channels;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.messages.Message;
import pt.isel.domain.messages.UpdatedMessage;
import pt.isel.domain.users.UserInfo;

import java.io.IOException;
import java.time.Instant;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SseUpdatedMessageEmitterAdapterTest {

    @Test
    void testEmitNewMessage() throws Exception {
        SseEmitter mockEmitter = mock(SseEmitter.class);
        SseUpdatedMessageEmitterAdapter adapter = new SseUpdatedMessageEmitterAdapter(mockEmitter);

        UserInfo user = new UserInfo(1L, "alice");
        Channel channel = new Channel(1L, "General", user);
        Message msg = new Message(10L, "Hello", user, channel);

        adapter.emit(new UpdatedMessage.NewMessage(msg));

        verify(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void testEmitKeepAlive() throws Exception {
        SseEmitter mockEmitter = mock(SseEmitter.class);
        SseUpdatedMessageEmitterAdapter adapter = new SseUpdatedMessageEmitterAdapter(mockEmitter);

        adapter.emit(new UpdatedMessage.KeepAlive(Instant.now()));

        verify(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void testComplete() {
        SseEmitter mockEmitter = mock(SseEmitter.class);
        SseUpdatedMessageEmitterAdapter adapter = new SseUpdatedMessageEmitterAdapter(mockEmitter);

        assertDoesNotThrow(adapter::complete);
        verify(mockEmitter).complete();
    }

    @Test
    void testEmitCatchesExceptionAndCompletesWithError() throws Exception {
        SseEmitter mockEmitter = mock(SseEmitter.class);
        SseUpdatedMessageEmitterAdapter adapter = new SseUpdatedMessageEmitterAdapter(mockEmitter);

        Mockito.doThrow(new IOException("Broken pipe")).when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

        assertDoesNotThrow(() -> adapter.emit(new UpdatedMessage.KeepAlive(Instant.now())));

        verify(mockEmitter).completeWithError(any(IOException.class));
    }

    @Test
    void testLifecycleCallbacksAreDelegated() {
        SseEmitter mockEmitter = mock(SseEmitter.class);
        SseUpdatedMessageEmitterAdapter adapter = new SseUpdatedMessageEmitterAdapter(mockEmitter);

        Runnable completionCallback = () -> {
        };
        Consumer<Throwable> errorCallback = e -> {
        };

        adapter.onCompletion(completionCallback);
        adapter.onError(errorCallback);

        verify(mockEmitter).onCompletion(completionCallback);
        verify(mockEmitter).onError(errorCallback);
    }
}