package pt.isel.api.invitations;

import pt.isel.domain.channels.AccessType;

import java.time.LocalDateTime;

public record InvitationInput(AccessType accessType, LocalDateTime expiresAt) {
}