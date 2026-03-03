package pt.isel.repositories.jdbi.security;

import org.jdbi.v3.core.Handle;
import pt.isel.repositories.jdbi.utils.JdbiUtils;
import pt.isel.repositories.security.TokenBlacklistRepository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

public class TokenBlacklistRepositoryJdbi implements TokenBlacklistRepository {
    private final Handle handle;

    public TokenBlacklistRepositoryJdbi(Handle handle) {
        this.handle = handle;
    }

    @Override
    public void add(String jti, LocalDateTime expiresAt) {
        JdbiUtils.executeUpdate(handle, """
                INSERT INTO token_blacklist (jti, expires_at)
                VALUES (:jti, :expires_at)
                ON CONFLICT (jti) DO NOTHING
                """, JdbiUtils.params("jti", jti, "expires_at", expiresAt));
    }

    @Override
    public boolean exists(String jti) {
        return handle.createQuery("SELECT 1 FROM token_blacklist WHERE jti = :jti")
                .bind("jti", jti)
                .mapTo(Integer.class)
                .findOne()
                .isPresent();
    }

    @Override
    public void cleanupExpired() {
        JdbiUtils.executeUpdate(handle, "DELETE FROM token_blacklist WHERE expires_at < :now",
                Map.of("now", LocalDateTime.now(ZoneOffset.UTC)));
    }

    @Override
    public void clear() {
        JdbiUtils.executeUpdate(handle, "DELETE FROM token_blacklist", Map.of());
    }
}