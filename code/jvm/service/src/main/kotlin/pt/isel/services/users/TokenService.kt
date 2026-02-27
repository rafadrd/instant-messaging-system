package pt.isel.services.users

import pt.isel.domain.security.TokenExternalInfo
import java.time.LocalDateTime

data class ParsedToken(
    val jti: String,
    val userId: Long,
    val expiresAt: LocalDateTime,
)

interface TokenService {
    fun createToken(userId: Long): TokenExternalInfo

    fun validateToken(token: String): ParsedToken?
}
