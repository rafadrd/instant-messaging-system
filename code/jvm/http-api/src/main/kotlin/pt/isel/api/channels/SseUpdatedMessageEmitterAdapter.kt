package pt.isel.api.channels

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import pt.isel.domain.messages.KeepAlive
import pt.isel.domain.messages.NewMessage
import pt.isel.domain.messages.UpdatedMessage
import pt.isel.domain.messages.UpdatedMessageEmitter

class SseUpdatedMessageEmitterAdapter(
    private val sseEmitter: SseEmitter,
) : UpdatedMessageEmitter {
    override fun emit(signal: UpdatedMessage) {
        val sseEvent =
            when (signal) {
                is NewMessage -> {
                    SseEmitter
                        .event()
                        .id(signal.message.id.toString())
                        .name("new-message")
                        .data(signal.message)
                }

                is KeepAlive -> {
                    SseEmitter
                        .event()
                        .comment("keep-alive: ${signal.timestamp.epochSecond}")
                }
            }
        try {
            sseEmitter.send(sseEvent)
        } catch (e: Exception) {
            sseEmitter.completeWithError(e)
        }
    }

    override fun onCompletion(callback: () -> Unit) {
        sseEmitter.onCompletion(callback)
    }

    override fun onError(callback: (Throwable) -> Unit) {
        sseEmitter.onError(callback)
    }
}
