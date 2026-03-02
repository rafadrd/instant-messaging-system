package pt.isel.services.users;

import java.time.LocalDateTime;

public record ParsedToken(
        String jti,
        Long userId,
        LocalDateTime expiresAt
) {
}