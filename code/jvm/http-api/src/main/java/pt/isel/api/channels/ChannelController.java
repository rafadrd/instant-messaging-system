package pt.isel.api.channels;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pt.isel.api.common.ErrorHandling;
import pt.isel.domain.users.AuthenticatedUser;
import pt.isel.services.channels.ChannelService;
import pt.isel.services.messages.MessageEventService;
import pt.isel.services.users.TicketService;

import java.util.Map;

@RestController
@RequestMapping("/api/channels")
public class ChannelController {
    private final ChannelService channelService;
    private final MessageEventService messageEventService;
    private final TicketService ticketService;

    public ChannelController(ChannelService channelService, MessageEventService messageEventService, TicketService ticketService) {
        this.channelService = channelService;
        this.messageEventService = messageEventService;
        this.ticketService = ticketService;
    }


    @GetMapping
    public ResponseEntity<?> getChannels(
            AuthenticatedUser user,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return ErrorHandling.handleResult(channelService.searchChannels(query, limit, offset));
    }

    @PostMapping
    public ResponseEntity<?> createChannel(AuthenticatedUser user, @Valid @RequestBody ChannelInput channel) {
        return ErrorHandling.handleResult(
                channelService.createChannel(channel.name(), user.user().id(), channel.isPublic()),
                createdChannel -> ResponseEntity.status(HttpStatus.CREATED).body(createdChannel)
        );
    }

    @GetMapping("/{channelId}")
    public ResponseEntity<?> getChannelById(AuthenticatedUser user, @PathVariable Long channelId) {
        return ErrorHandling.handleResult(channelService.getChannelById(channelId));
    }

    @PutMapping("/{channelId}")
    public ResponseEntity<?> editChannel(AuthenticatedUser user, @PathVariable Long channelId, @Valid @RequestBody EditChannelInput input) {
        return ErrorHandling.handleResult(channelService.editChannel(user.user().id(), channelId, input.name(), input.isPublic()));
    }

    @DeleteMapping("/{channelId}")
    public ResponseEntity<?> deleteChannel(AuthenticatedUser user, @PathVariable Long channelId) {
        return ErrorHandling.handleResult(channelService.deleteChannel(user.user().id(), channelId));
    }

    @PostMapping("/{channelId}/join")
    public ResponseEntity<?> joinChannel(AuthenticatedUser user, @PathVariable Long channelId) {
        return ErrorHandling.handleResult(channelService.joinPublicChannel(user.user().id(), channelId));
    }

    @PostMapping("/join-by-token")
    public ResponseEntity<?> joinChannelByToken(AuthenticatedUser user, @Valid @RequestBody JoinByTokenInput input) {
        return ErrorHandling.handleResult(channelService.joinPrivateChannel(user.user().id(), input.token()));
    }

    @PostMapping("/{channelId}/leave")
    public ResponseEntity<?> leaveChannel(AuthenticatedUser user, @PathVariable Long channelId) {
        return ErrorHandling.handleResult(channelService.leaveChannel(channelId, user.user().id()));
    }

    @GetMapping("/{channelId}/members")
    public ResponseEntity<?> getMembers(AuthenticatedUser user, @PathVariable Long channelId,
                                        @RequestParam(defaultValue = "50") int limit,
                                        @RequestParam(defaultValue = "0") int offset) {
        return ErrorHandling.handleResult(channelService.getUsersInChannel(channelId, limit, offset));
    }

    @GetMapping("/{channelId}/members/{userId}")
    public ResponseEntity<?> getAccessType(AuthenticatedUser user, @PathVariable Long channelId, @PathVariable Long userId) {
        return ErrorHandling.handleResult(channelService.getAccessType(user.user().id(), userId, channelId));
    }

    @PutMapping("/{channelId}/members/{userId}")
    public ResponseEntity<?> editMemberAccess(AuthenticatedUser user, @PathVariable Long channelId, @PathVariable Long userId, @Valid @RequestBody EditMemberInput input) {
        return ErrorHandling.handleResult(channelService.editMemberAccess(user.user().id(), channelId, userId, input.accessType()));
    }

    @PostMapping("/{channelId}/socket-ticket")
    public ResponseEntity<?> getSocketTicket(AuthenticatedUser user, @PathVariable Long channelId) {
        return ErrorHandling.handleResult(
                channelService.getAccessType(user.user().id(), user.user().id(), channelId),
                access -> {
                    String ticket = ticketService.createTicket(user.user().id());
                    return ResponseEntity.ok(Map.of("ticket", ticket));
                }
        );
    }

    @GetMapping("/{channelId}/listen")
    public ResponseEntity<?> listen(AuthenticatedUser user, @PathVariable Long channelId) {
        return ErrorHandling.handleResult(channelService.getAccessType(user.user().id(), user.user().id(), channelId), accessType -> {
            SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
            emitter.onTimeout(emitter::complete);
            try {
                var adapter = new SseUpdatedMessageEmitterAdapter(emitter);
                messageEventService.addEmitter(channelId, user.user().id(), adapter);
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
            return ResponseEntity.ok(emitter);
        });
    }
}