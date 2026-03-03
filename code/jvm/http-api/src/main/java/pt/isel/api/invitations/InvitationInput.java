package pt.isel.api.invitations;

import jakarta.validation.constraints.NotNull;
import pt.isel.domain.channels.AccessType;

import java.time.LocalDateTime;

public record InvitationInput(@NotNull AccessType accessType, @NotNull LocalDateTime expiresAt) {
}