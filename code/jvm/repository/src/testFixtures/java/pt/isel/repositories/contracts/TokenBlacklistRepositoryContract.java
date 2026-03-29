package pt.isel.repositories.contracts;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public interface TokenBlacklistRepositoryContract extends RepositoryTestHelper {

    @Test
    default void Add_ValidToken_AddsAndExists() {
        getTxManager().run(trx -> {
            String jti = "token-uuid-123";
            assertThat(trx.repoTokenBlacklist().exists(jti)).isFalse();

            trx.repoTokenBlacklist().add(jti, LocalDateTime.now(ZoneOffset.UTC).plusHours(1));

            assertThat(trx.repoTokenBlacklist().exists(jti)).isTrue();
            return null;
        });
    }

    @Test
    default void Clear_HasRecords_RemovesAllRecords() {
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
    default void CleanupExpired_HasExpiredTokens_RemovesExpiredTokens() {
        getTxManager().run(trx -> {
            trx.repoTokenBlacklist().add("expired-token", LocalDateTime.now(ZoneOffset.UTC).minusHours(1));
            trx.repoTokenBlacklist().add("valid-token", LocalDateTime.now(ZoneOffset.UTC).plusHours(1));

            trx.repoTokenBlacklist().cleanupExpired(LocalDateTime.now(ZoneOffset.UTC));

            assertThat(trx.repoTokenBlacklist().exists("expired-token")).isFalse();
            assertThat(trx.repoTokenBlacklist().exists("valid-token")).isTrue();
            return null;
        });
    }

    @Test
    default void Add_DuplicateToken_DoesNotThrow() {
        getTxManager().run(trx -> {
            String jti = "duplicate-token";
            LocalDateTime expiry = LocalDateTime.now(ZoneOffset.UTC).plusHours(1);

            assertThatCode(() -> {
                trx.repoTokenBlacklist().add(jti, expiry);
                trx.repoTokenBlacklist().add(jti, expiry);
            }).doesNotThrowAnyException();

            assertThat(trx.repoTokenBlacklist().exists(jti)).isTrue();
            return null;
        });
    }
}