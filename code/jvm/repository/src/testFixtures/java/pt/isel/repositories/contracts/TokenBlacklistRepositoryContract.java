package pt.isel.repositories.contracts;

import org.junit.jupiter.api.Test;
import pt.isel.repositories.TransactionManager;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public interface TokenBlacklistRepositoryContract {
    TransactionManager getTxManager();

    @Test
    default void testAddAndExists() {
        getTxManager().run(trx -> {
            String jti = "token-uuid-123";
            assertFalse(trx.repoTokenBlacklist().exists(jti));

            trx.repoTokenBlacklist().add(jti, LocalDateTime.now(ZoneOffset.UTC).plusHours(1));
            assertTrue(trx.repoTokenBlacklist().exists(jti));
            return null;
        });
    }

    @Test
    default void testClear() {
        getTxManager().run(trx -> {
            trx.repoTokenBlacklist().add("t1", LocalDateTime.now(ZoneOffset.UTC));
            trx.repoTokenBlacklist().add("t2", LocalDateTime.now(ZoneOffset.UTC));

            trx.repoTokenBlacklist().clear();

            assertFalse(trx.repoTokenBlacklist().exists("t1"));
            assertFalse(trx.repoTokenBlacklist().exists("t2"));
            return null;
        });
    }

    @Test
    default void testCleanupExpired() {
        getTxManager().run(trx -> {
            trx.repoTokenBlacklist().add("expired-token", LocalDateTime.now(ZoneOffset.UTC).minusHours(1));
            trx.repoTokenBlacklist().add("valid-token", LocalDateTime.now(ZoneOffset.UTC).plusHours(1));

            trx.repoTokenBlacklist().cleanupExpired();

            assertFalse(trx.repoTokenBlacklist().exists("expired-token"));
            assertTrue(trx.repoTokenBlacklist().exists("valid-token"));
            return null;
        });
    }

    @Test
    default void testAddDuplicateDoesNotThrow() {
        getTxManager().run(trx -> {
            String jti = "duplicate-token";
            LocalDateTime expiry = LocalDateTime.now(ZoneOffset.UTC).plusHours(1);

            assertDoesNotThrow(() -> {
                trx.repoTokenBlacklist().add(jti, expiry);
                trx.repoTokenBlacklist().add(jti, expiry); // Should ignore conflict
            });

            assertTrue(trx.repoTokenBlacklist().exists(jti));
            return null;
        });
    }
}