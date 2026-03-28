package pt.isel.repositories.security;

import java.time.LocalDateTime;

public interface TokenBlacklistRepository {
    void add(String jti, LocalDateTime expiresAt);

    boolean exists(String jti);

    void cleanupExpired(LocalDateTime now);

    void clear();
}