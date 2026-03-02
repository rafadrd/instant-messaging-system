package pt.isel.domain.messages;

import java.time.Instant;

public sealed interface UpdatedMessage {
    record NewMessage(Message message) implements UpdatedMessage {
    }

    record KeepAlive(Instant timestamp) implements UpdatedMessage {
    }
}