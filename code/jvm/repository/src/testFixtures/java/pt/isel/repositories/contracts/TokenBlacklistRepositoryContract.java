package pt.isel.repositories.contracts;

import org.junit.jupiter.api.Test;
import pt.isel.repositories.TransactionManager;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public interface TokenBlacklistRepositoryContract {
    TransactionManager getTxManager();

    @Test
    default void testAddAndExists() {
        getTxManager().run(trx -> {
            String jti = "token-uuid-123";
            assertThat(trx.repoTokenBlacklist().exists(jti)).isFalse();

            trx.repoTokenBlacklist().add(jti, LocalDateTime.now(ZoneOffset.UTC).plusHours(1));
            assertThat(trx.repoTokenBlacklist().exists(jti)).isTrue();
            return null;
        });
    }

    @Test
    default void testClear() {
        getTxManager().run(trx -> {
            trx.repoTokenBlacklist().add("t1", LocalDateTime.now(ZoneOffset.UTC));
            trx.repoTokenBlacklist().add("t2", LocalDateTime.now(ZoneOffset.UTC));

            trx.repoTokenBlacklist().clear();

            assertThat(trx.repoTokenBlacklist().exists("t1")).isFalse();
            assertThat(trx.repoTokenBlacklist().exists("t2")).isFalse();
            return null;
        });
    }

    @Test
    default void testCleanupExpired() {
        getTxManager().run(trx -> {
            trx.repoTokenBlacklist().add("expired-token", LocalDateTime.now(ZoneOffset.UTC).minusHours(1));
            trx.repoTokenBlacklist().add("valid-token", LocalDateTime.now(ZoneOffset.UTC).plusHours(1));

            trx.repoTokenBlacklist().cleanupExpired();

            assertThat(trx.repoTokenBlacklist().exists("expired-token")).isFalse();
            assertThat(trx.repoTokenBlacklist().exists("valid-token")).isTrue();
            return null;
        });
    }

    @Test
    default void testAddDuplicateDoesNotThrow() {
        getTxManager().run(trx -> {
            String jti = "duplicate-token";
            LocalDateTime expiry = LocalDateTime.now(ZoneOffset.UTC).plusHours(1);

            assertThatCode(() -> {
                trx.repoTokenBlacklist().add(jti, expiry);
                trx.repoTokenBlacklist().add(jti, expiry); // Should ignore conflict
            }).doesNotThrowAnyException();

            assertThat(trx.repoTokenBlacklist().exists(jti)).isTrue();
            return null;
        });
    }
}