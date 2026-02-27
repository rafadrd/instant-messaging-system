package pt.isel.api.invitations

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pt.isel.api.common.handleResult
import pt.isel.domain.users.AuthenticatedUser
import pt.isel.services.invitations.InvitationService

@RestController
@RequestMapping("/api/channels/{channelId}/invitations")
class InvitationController(
    private val invitationService: InvitationService,
) {
    @PostMapping
    fun createInvitation(
        user: AuthenticatedUser,
        @PathVariable channelId: Long,
        @RequestBody invitation: InvitationInput,
    ): ResponseEntity<*> =
        handleResult(
            invitationService.createInvitation(
                creatorId = user.user.id,
                channelId = channelId,
                accessType = invitation.accessType,
                expiresAt = invitation.expiresAt,
            ),
        )

    @GetMapping
    fun getInvitationsForChannel(
        user: AuthenticatedUser,
        @PathVariable channelId: Long,
    ): ResponseEntity<*> =
        handleResult(
            invitationService.getInvitationsForChannel(
                requesterId = user.user.id,
                channelId = channelId,
            ),
        )

    @PostMapping("/{invitationId}/revoke")
    fun revokeInvitation(
        user: AuthenticatedUser,
        @PathVariable channelId: Long,
        @PathVariable invitationId: Long,
    ): ResponseEntity<*> =
        handleResult(
            invitationService.revokeInvitation(
                userId = user.user.id,
                channelId = channelId,
                invitationId = invitationId,
            ),
        )
}
