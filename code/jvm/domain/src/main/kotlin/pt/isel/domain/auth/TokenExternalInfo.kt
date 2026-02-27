package pt.isel.domain.auth

import java.time.Instant

data class TokenExternalInfo(
    val tokenValue: String,
    val tokenExpiration: Instant,
    val userId: Long,
)
