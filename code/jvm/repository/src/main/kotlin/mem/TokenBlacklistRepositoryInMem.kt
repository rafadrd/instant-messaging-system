package pt.isel.mem

import jakarta.inject.Named
import pt.isel.TokenBlacklistRepository
import java.time.LocalDateTime

@Named
class TokenBlacklistRepositoryInMem : TokenBlacklistRepository {
    private val blacklistedTokens = mutableSetOf<String>()

    override fun add(
        jti: String,
        expiresAt: LocalDateTime,
    ) {
        blacklistedTokens.add(jti)
    }

    override fun exists(jti: String): Boolean = jti in blacklistedTokens

    override fun clear() {
        blacklistedTokens.clear()
    }
}
