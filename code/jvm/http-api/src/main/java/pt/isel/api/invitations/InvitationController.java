package pt.isel.api.invitations;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.isel.api.common.ErrorHandling;
import pt.isel.domain.users.AuthenticatedUser;
import pt.isel.services.invitations.InvitationService;

@RestController
@RequestMapping("/api/channels/{channelId}/invitations")
public class InvitationController {
    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping
    public ResponseEntity<?> createInvitation(AuthenticatedUser user, @PathVariable Long channelId, @RequestBody InvitationInput invitation) {
        return ErrorHandling.handleResult(invitationService.createInvitation(
                user.user().id(), channelId, invitation.accessType(), invitation.expiresAt()
        ));
    }

    @GetMapping
    public ResponseEntity<?> getInvitationsForChannel(AuthenticatedUser user, @PathVariable Long channelId) {
        return ErrorHandling.handleResult(invitationService.getInvitationsForChannel(user.user().id(), channelId));
    }

    @PostMapping("/{invitationId}/revoke")
    public ResponseEntity<?> revokeInvitation(AuthenticatedUser user, @PathVariable Long channelId, @PathVariable Long invitationId) {
        return ErrorHandling.handleResult(invitationService.revokeInvitation(user.user().id(), channelId, invitationId));
    }
}