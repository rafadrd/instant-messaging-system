package pt.isel.api.channels;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pt.isel.domain.builders.MessageBuilder;
import pt.isel.domain.messages.Message;
import pt.isel.domain.messages.UpdatedMessage;

import java.io.IOException;
import java.time.Instant;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SseUpdatedMessageEmitterAdapterTest {

    @Mock
    private SseEmitter mockEmitter;

    @InjectMocks
    private SseUpdatedMessageEmitterAdapter adapter;

    @Test
    void testEmitNewMessage() throws Exception {
        Message msg = new MessageBuilder().withId(10L).withContent("Hello").build();

        adapter.emit(new UpdatedMessage.NewMessage(msg));

        verify(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void testEmitKeepAlive() throws Exception {
        adapter.emit(new UpdatedMessage.KeepAlive(Instant.now()));

        verify(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void testComplete() {
        assertThatCode(adapter::complete).doesNotThrowAnyException();
        verify(mockEmitter).complete();
    }

    @Test
    void testEmitCatchesExceptionAndCompletesWithError() throws Exception {
        Mockito.doThrow(new IOException("Broken pipe")).when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

        assertThatCode(() -> adapter.emit(new UpdatedMessage.KeepAlive(Instant.now()))).doesNotThrowAnyException();

        verify(mockEmitter).completeWithError(any(IOException.class));
    }

    @Test
    void testLifecycleCallbacksAreDelegated() {
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