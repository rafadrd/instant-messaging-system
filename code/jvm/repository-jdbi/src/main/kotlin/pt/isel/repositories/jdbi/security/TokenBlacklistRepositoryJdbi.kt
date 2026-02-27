package pt.isel.repositories.jdbi.security

import org.jdbi.v3.core.Handle
import pt.isel.repositories.jdbi.utils.executeUpdate
import pt.isel.repositories.security.TokenBlacklistRepository
import java.time.LocalDateTime

class TokenBlacklistRepositoryJdbi(
    private val handle: Handle,
) : TokenBlacklistRepository {
    override fun add(
        jti: String,
        expiresAt: LocalDateTime,
    ) {
        handle.executeUpdate(
            """
            INSERT INTO dbo.token_blacklist (jti, expires_at)
            VALUES (:jti, :expires_at)
            ON CONFLICT (jti) DO NOTHING
            """,
            mapOf("jti" to jti, "expires_at" to expiresAt),
        )
    }

    override fun exists(jti: String): Boolean =
        handle
            .createQuery("SELECT 1 FROM dbo.token_blacklist WHERE jti = :jti")
            .bind("jti", jti)
            .mapTo(Int::class.java)
            .findOne()
            .isPresent

    override fun clear() {
        handle.executeUpdate("DELETE FROM dbo.token_blacklist")
    }
}
