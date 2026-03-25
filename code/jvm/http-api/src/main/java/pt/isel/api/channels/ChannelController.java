package pt.isel.api.channels;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pt.isel.api.common.ErrorHandling;
import pt.isel.api.common.PageInput;
import pt.isel.domain.users.AuthenticatedUser;
import pt.isel.services.channels.ChannelService;
import pt.isel.services.messages.MessageEventService;
import pt.isel.services.users.TicketService;

import java.util.Map;

@RestController
@RequestMapping("/api/channels")
@Tag(name = "Channels", description = "Channels management")
@SecurityRequirement(name = "BearerAuth")
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
    @Operation(summary = "Get list of channels", description = "Retrieves a list of channels optionally filtered by a query")
    public ResponseEntity<?> getChannels(
            @Parameter(hidden = true) AuthenticatedUser user,
            @RequestParam(defaultValue = "") String query,
            PageInput page) {
        return ErrorHandling.handleResult(channelService.searchChannels(query, page.limit(), page.offset()));
    }

    @PostMapping
    @Operation(summary = "Create a new channel")
    public ResponseEntity<?> createChannel(@Parameter(hidden = true) AuthenticatedUser user, @Valid @RequestBody ChannelInput channel) {
        return ErrorHandling.handleResult(
                channelService.createChannel(channel.name(), user.user().id(), channel.isPublic()),
                createdChannel -> ResponseEntity.status(HttpStatus.CREATED).body(createdChannel)
        );
    }

    @GetMapping("/{channelId}")
    @Operation(summary = "Get channel by ID")
    public ResponseEntity<?> getChannelById(@Parameter(hidden = true) AuthenticatedUser user, @PathVariable Long channelId) {
        return ErrorHandling.handleResult(channelService.getChannelById(channelId));
    }

    @PutMapping("/{channelId}")
    @Operation(summary = "Edit channel details")
    public ResponseEntity<?> editChannel(@Parameter(hidden = true) AuthenticatedUser user, @PathVariable Long channelId, @Valid @RequestBody EditChannelInput input) {
        return ErrorHandling.handleResult(channelService.editChannel(user.user().id(), channelId, input.name(), input.isPublic()));
    }

    @DeleteMapping("/{channelId}")
    @Operation(summary = "Delete a channel")
    public ResponseEntity<?> deleteChannel(@Parameter(hidden = true) AuthenticatedUser user, @PathVariable Long channelId) {
        return ErrorHandling.handleResult(channelService.deleteChannel(user.user().id(), channelId));
    }

    @PostMapping("/{channelId}/join")
    @Operation(summary = "Join a public channel by ID")
    public ResponseEntity<?> joinChannel(@Parameter(hidden = true) AuthenticatedUser user, @PathVariable Long channelId) {
        return ErrorHandling.handleResult(channelService.joinPublicChannel(user.user().id(), channelId));
    }

    @PostMapping("/join-by-token")
    @Operation(summary = "Join a private channel using an invitation token")
    public ResponseEntity<?> joinChannelByToken(@Parameter(hidden = true) AuthenticatedUser user, @Valid @RequestBody JoinByTokenInput input) {
        return ErrorHandling.handleResult(channelService.joinPrivateChannel(user.user().id(), input.token()));
    }

    @PostMapping("/{channelId}/leave")
    @Operation(summary = "Leave a channel")
    public ResponseEntity<?> leaveChannel(@Parameter(hidden = true) AuthenticatedUser user, @PathVariable Long channelId) {
        return ErrorHandling.handleResult(channelService.leaveChannel(channelId, user.user().id()));
    }

    @GetMapping("/{channelId}/members")
    @Operation(summary = "Get members of a channel")
    public ResponseEntity<?> getMembers(@Parameter(hidden = true) AuthenticatedUser user, @PathVariable Long channelId, PageInput page) {
        return ErrorHandling.handleResult(channelService.getUsersInChannel(channelId, page.limit(), page.offset()));
    }

    @GetMapping("/{channelId}/members/{userId}")
    @Operation(summary = "Get access type of a member")
    public ResponseEntity<?> getAccessType(@Parameter(hidden = true) AuthenticatedUser user, @PathVariable Long channelId, @PathVariable Long userId) {
        return ErrorHandling.handleResult(channelService.getAccessType(user.user().id(), userId, channelId));
    }

    @PutMapping("/{channelId}/members/{userId}")
    @Operation(summary = "Edit member access type")
    public ResponseEntity<?> editMemberAccess(@Parameter(hidden = true) AuthenticatedUser user, @PathVariable Long channelId, @PathVariable Long userId, @Valid @RequestBody EditMemberInput input) {
        return ErrorHandling.handleResult(channelService.editMemberAccess(user.user().id(), channelId, userId, input.accessType()));
    }

    @PostMapping("/{channelId}/socket-ticket")
    @Operation(summary = "Get an SSE stream connection ticket")
    public ResponseEntity<?> getSocketTicket(@Parameter(hidden = true) AuthenticatedUser user, @PathVariable Long channelId) {
        return ErrorHandling.handleResult(
                channelService.getAccessType(user.user().id(), user.user().id(), channelId),
                access -> {
                    String ticket = ticketService.createTicket(user.user().id());
                    return ResponseEntity.ok(Map.of("ticket", ticket));
                }
        );
    }

    @GetMapping("/{channelId}/listen")
    @Operation(summary = "Listen for real-time messages via SSE")
    public ResponseEntity<?> listen(@Parameter(hidden = true) AuthenticatedUser user, @PathVariable Long channelId) {
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