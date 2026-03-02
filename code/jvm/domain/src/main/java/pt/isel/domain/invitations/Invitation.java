package pt.isel.domain.invitations;

import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.users.UserInfo;

import java.time.LocalDateTime;

public record Invitation(
        Long id,
        String token,
        UserInfo createdBy,
        Channel channel,
        AccessType accessType,
        LocalDateTime expiresAt,
        InvitationStatus status
) {
    public Invitation(Long id, String token, UserInfo createdBy, Channel channel, AccessType accessType, LocalDateTime expiresAt) {
        this(id, token, createdBy, channel, accessType, expiresAt, InvitationStatus.PENDING);
    }
}