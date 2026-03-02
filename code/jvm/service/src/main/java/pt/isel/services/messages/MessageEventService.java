package pt.isel.services.messages;

import pt.isel.domain.messages.UpdatedMessage;
import pt.isel.domain.messages.UpdatedMessageEmitter;

public interface MessageEventService {
    void addEmitter(Long channelId, Long userId, UpdatedMessageEmitter emitter);

    void broadcastMessage(Long channelId, UpdatedMessage signal);
}