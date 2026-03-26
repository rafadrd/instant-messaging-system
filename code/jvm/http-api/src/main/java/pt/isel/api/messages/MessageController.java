package pt.isel.api.messages;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Messages", description = "Messages management")
@SecurityRequirement(name = "BearerAuth")
public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    @Operation(summary = "Create a new message in a channel")
    public ResponseEntity<?> createMessage(@Parameter(hidden = true) AuthenticatedUser user, @PathVariable("channelId") Long channelId, @Valid @RequestBody MessageRequest request) {
        return ErrorHandling.handleResult(
                messageService.createMessage(request.content(), user.user().id(), channelId),
                message -> ResponseEntity.status(HttpStatus.CREATED).body(message)
        );
    }

    @GetMapping
    @Operation(summary = "Get messages in a channel")
    public ResponseEntity<?> getMessages(@Parameter(hidden = true) AuthenticatedUser user, @PathVariable("channelId") Long channelId, PageInput page) {
        return ErrorHandling.handleResult(messageService.getMessagesInChannel(user.user().id(), channelId, page.limit(), page.offset()));
    }
}