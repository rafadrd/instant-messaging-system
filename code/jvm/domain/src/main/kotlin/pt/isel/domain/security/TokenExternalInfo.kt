package pt.isel.domain.security

import java.time.Instant

data class TokenExternalInfo(
    val tokenValue: String,
    val tokenExpiration: Instant,
    val userId: Long,
)
