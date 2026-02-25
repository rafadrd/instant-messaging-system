package pt.isel.repositories

import java.time.LocalDateTime

interface TokenBlacklistRepository {
    fun add(
        jti: String,
        expiresAt: LocalDateTime,
    )

    fun exists(jti: String): Boolean

    fun clear()
}
