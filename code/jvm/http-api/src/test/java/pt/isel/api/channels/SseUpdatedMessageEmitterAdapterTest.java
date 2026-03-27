package pt.isel.api.channels;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pt.isel.domain.builders.MessageBuilder;
import pt.isel.domain.messages.Message;
import pt.isel.domain.messages.UpdatedMessage;

import java.io.IOException;
import java.time.Instant;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SseUpdatedMessageEmitterAdapterTest {

    @Test
    void testEmitNewMessage() throws Exception {
        SseEmitter mockEmitter = mock(SseEmitter.class);
        SseUpdatedMessageEmitterAdapter adapter = new SseUpdatedMessageEmitterAdapter(mockEmitter);

        Message msg = new MessageBuilder().withId(10L).withContent("Hello").build();

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

        assertThatCode(adapter::complete).doesNotThrowAnyException();
        verify(mockEmitter).complete();
    }

    @Test
    void testEmitCatchesExceptionAndCompletesWithError() throws Exception {
        SseEmitter mockEmitter = mock(SseEmitter.class);
        SseUpdatedMessageEmitterAdapter adapter = new SseUpdatedMessageEmitterAdapter(mockEmitter);

        Mockito.doThrow(new IOException("Broken pipe")).when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

        assertThatCode(() -> adapter.emit(new UpdatedMessage.KeepAlive(Instant.now()))).doesNotThrowAnyException();

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