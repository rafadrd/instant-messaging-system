package pt.isel.api.messages;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.isel.api.common.ErrorHandling;
import pt.isel.domain.users.AuthenticatedUser;
import pt.isel.services.messages.MessageService;

@RestController
@RequestMapping("/api/channels/{channelId}/messages")
public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public ResponseEntity<?> createMessage(AuthenticatedUser user, @PathVariable Long channelId, @Valid @RequestBody MessageRequest request) {
        return ErrorHandling.handleResult(
                messageService.createMessage(request.content(), user.user().id(), channelId),
                message -> ResponseEntity.status(HttpStatus.CREATED).body(message)
        );
    }

    @GetMapping
    public ResponseEntity<?> getMessages(AuthenticatedUser user, @PathVariable Long channelId,
                                         @RequestParam(defaultValue = "50") int limit,
                                         @RequestParam(defaultValue = "0") int offset) {
        return ErrorHandling.handleResult(messageService.getMessagesInChannel(user.user().id(), channelId, limit, offset));
    }
}