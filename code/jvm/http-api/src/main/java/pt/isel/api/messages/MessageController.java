package pt.isel.api.messages;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.isel.api.common.ErrorHandling;
import pt.isel.api.common.PageInput;
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
    public ResponseEntity<?> getMessages(AuthenticatedUser user, @PathVariable Long channelId, PageInput page) {
        return ErrorHandling.handleResult(messageService.getMessagesInChannel(user.user().id(), channelId, page.limit(), page.offset()));
    }
}