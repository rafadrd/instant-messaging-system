package pt.isel.api.channels;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pt.isel.domain.messages.UpdatedMessage;
import pt.isel.domain.messages.UpdatedMessageEmitter;

import java.util.function.Consumer;

public class SseUpdatedMessageEmitterAdapter implements UpdatedMessageEmitter {
    private final SseEmitter sseEmitter;

    public SseUpdatedMessageEmitterAdapter(SseEmitter sseEmitter) {
        this.sseEmitter = sseEmitter;
    }

    @Override
    public void emit(UpdatedMessage signal) {
        SseEmitter.SseEventBuilder sseEvent = switch (signal) {
            case UpdatedMessage.NewMessage msg -> SseEmitter.event()
                    .id(msg.message().id().toString())
                    .name("new-message")
                    .data(msg.message());
            case UpdatedMessage.KeepAlive keep -> SseEmitter.event()
                    .comment("keep-alive: " + keep.timestamp().getEpochSecond());
        };

        try {
            sseEmitter.send(sseEvent);
        } catch (Exception e) {
            sseEmitter.completeWithError(e);
        }
    }

    @Override
    public void onCompletion(Runnable callback) {
        sseEmitter.onCompletion(callback);
    }

    @Override
    public void onError(Consumer<Throwable> callback) {
        sseEmitter.onError(callback);
    }
}