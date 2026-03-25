package pt.isel.api.invitations;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.isel.api.common.ErrorHandling;
import pt.isel.domain.users.AuthenticatedUser;
import pt.isel.services.invitations.InvitationService;

@RestController
@RequestMapping("/api/channels/{channelId}/invitations")
@Tag(name = "Invitations", description = "Invitations management")
@SecurityRequirement(name = "BearerAuth")
public class InvitationController {
    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping
    @Operation(summary = "Create an invitation for a channel")
    public ResponseEntity<?> createInvitation(@Parameter(hidden = true) AuthenticatedUser user, @PathVariable Long channelId, @RequestBody InvitationInput invitation) {
        return ErrorHandling.handleResult(invitationService.createInvitation(
                user.user().id(), channelId, invitation.accessType(), invitation.expiresAt()
        ));
    }

    @GetMapping
    @Operation(summary = "Get invitations for a channel")
    public ResponseEntity<?> getInvitationsForChannel(@Parameter(hidden = true) AuthenticatedUser user, @PathVariable Long channelId) {
        return ErrorHandling.handleResult(invitationService.getInvitationsForChannel(user.user().id(), channelId));
    }

    @PostMapping("/{invitationId}/revoke")
    @Operation(summary = "Revoke an active invitation")
    public ResponseEntity<?> revokeInvitation(@Parameter(hidden = true) AuthenticatedUser user, @PathVariable Long channelId, @PathVariable Long invitationId) {
        return ErrorHandling.handleResult(invitationService.revokeInvitation(user.user().id(), channelId, invitationId));
    }
}