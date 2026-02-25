package pt.isel.infrastructure

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.WeakKeyException
import jakarta.annotation.PostConstruct
import kotlinx.datetime.Instant
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import pt.isel.domain.auth.TokenExternalInfo
import pt.isel.services.ParsedToken
import pt.isel.services.TokenService
import java.nio.charset.StandardCharsets
import java.time.ZoneId
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey
import kotlin.time.Duration.Companion.hours

@Component
class JwtTokenService(
    @Value("\${jwt.secret}") private val secret: String,
) : TokenService {
    private lateinit var key: SecretKey
    private val expirationTimeMillis = 24.hours.inWholeMilliseconds

    @PostConstruct
    fun init() {
        val bytes = secret.toByteArray(StandardCharsets.UTF_8)
        if (bytes.size * 8 < 256) {
            throw WeakKeyException("JWT_SECRET must be at least 256 bits (32 characters) long.")
        }
        this.key = Keys.hmacShaKeyFor(bytes)
    }

    override fun createToken(userId: Long): TokenExternalInfo {
        val now = System.currentTimeMillis()
        val expiration = Date(now + expirationTimeMillis)
        val jti = UUID.randomUUID().toString()
        val tokenValue =
            Jwts
                .builder()
                .subject(userId.toString())
                .id(jti)
                .issuedAt(Date(now))
                .expiration(expiration)
                .signWith(key)
                .compact()
        return TokenExternalInfo(
            tokenValue,
            Instant.fromEpochMilliseconds(expiration.time),
            userId,
        )
    }

    override fun validateToken(token: String): ParsedToken? =
        try {
            val claims =
                Jwts
                    .parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .payload

            val jti = claims.id ?: return null
            val userId = claims.subject?.toLongOrNull() ?: return null
            val expiration = claims.expiration ?: return null
            val expiresAt =
                expiration
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()

            ParsedToken(jti, userId, expiresAt)
        } catch (_: JwtException) {
            null
        }
}
