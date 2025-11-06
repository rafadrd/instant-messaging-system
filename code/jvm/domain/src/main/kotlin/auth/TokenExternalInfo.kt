package pt.isel.auth

import kotlinx.datetime.Instant

data class TokenExternalInfo(
    val tokenValue: String,
    val tokenExpiration: Instant,
    val userId: Long,
)
