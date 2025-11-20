package pt.isel

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.WeakKeyException
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import pt.isel.auth.TokenExternalInfo
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey
import kotlin.time.Duration.Companion.hours

@Component
class TokenService(
    @Value("\${jwt.secret}") private val secret: String,
) {
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

    fun createToken(userId: Long): TokenExternalInfo {
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
            kotlinx.datetime.Instant.fromEpochMilliseconds(expiration.time),
            userId,
        )
    }

    fun validateToken(token: String): Jws<Claims>? =
        try {
            Jwts
                .parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
        } catch (_: JwtException) {
            null
        }
}
