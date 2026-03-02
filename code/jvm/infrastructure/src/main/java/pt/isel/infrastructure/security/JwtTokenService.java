package pt.isel.infrastructure.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pt.isel.domain.security.TokenExternalInfo;
import pt.isel.services.users.ParsedToken;
import pt.isel.services.users.TokenService;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenService implements TokenService {

    private final String secret;
    private final long expirationTimeMillis = 24L * 60 * 60 * 1000; // 24 hours
    private SecretKey key;

    public JwtTokenService(@Value("${jwt.secret}") String secret) {
        this.secret = secret;
    }

    @PostConstruct
    public void init() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length * 8 < 256) {
            throw new WeakKeyException("JWT_SECRET must be at least 256 bits (32 characters) long.");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    @Override
    public TokenExternalInfo createToken(Long userId) {
        long now = System.currentTimeMillis();
        Date expiration = new Date(now + expirationTimeMillis);
        String jti = UUID.randomUUID().toString();

        String tokenValue = Jwts.builder()
                .subject(userId.toString())
                .id(jti)
                .issuedAt(new Date(now))
                .expiration(expiration)
                .signWith(key)
                .compact();

        return new TokenExternalInfo(
                tokenValue,
                Instant.ofEpochMilli(expiration.getTime()),
                userId
        );
    }

    @Override
    public ParsedToken validateToken(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String jti = claims.getId();
            if (jti == null) return null;

            String subject = claims.getSubject();
            if (subject == null) return null;
            Long userId;
            try {
                userId = Long.parseLong(subject);
            } catch (NumberFormatException e) {
                return null;
            }

            Date expiration = claims.getExpiration();
            if (expiration == null) return null;

            var expiresAt = expiration.toInstant()
                    .atZone(ZoneOffset.UTC)
                    .toLocalDateTime();

            return new ParsedToken(jti, userId, expiresAt);
        } catch (JwtException e) {
            return null;
        }
    }
}