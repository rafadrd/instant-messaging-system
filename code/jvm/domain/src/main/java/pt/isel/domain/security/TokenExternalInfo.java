package pt.isel.domain.security;

import java.time.Instant;

public record TokenExternalInfo(
        String tokenValue,
        Instant tokenExpiration,
        Long userId
) {
}