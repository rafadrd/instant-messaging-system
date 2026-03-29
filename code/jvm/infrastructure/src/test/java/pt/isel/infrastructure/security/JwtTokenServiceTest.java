package pt.isel.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;
import pt.isel.domain.security.TokenExternalInfo;
import pt.isel.services.users.ParsedToken;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private static final String VALID_SECRET;
    private static final String INVALID_SECRET = "short-secret";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneOffset.UTC);

    static {
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        VALID_SECRET = Base64.getEncoder().encodeToString(keyBytes);
    }

    @Test
    void Init_ShortSecret_ThrowsException() {
        JwtTokenService service = new JwtTokenService(INVALID_SECRET, CLOCK);

        assertThatThrownBy(service::init).isInstanceOf(WeakKeyException.class);
    }

    @Test
    void CreateAndValidateToken_ValidInput_ReturnsParsedToken() {
        JwtTokenService service = new JwtTokenService(VALID_SECRET, CLOCK);
        service.init();
        Long userId = 123L;

        TokenExternalInfo tokenInfo = service.createToken(userId);
        ParsedToken parsedToken = service.validateToken(tokenInfo.tokenValue());

        assertThat(tokenInfo).isNotNull();
        assertThat(tokenInfo.tokenValue()).isNotNull();
        assertThat(tokenInfo.userId()).isEqualTo(userId);
        assertThat(tokenInfo.tokenExpiration()).isNotNull();
        assertThat(parsedToken).isNotNull();
        assertThat(parsedToken.userId()).isEqualTo(userId);
        assertThat(parsedToken.jti()).isNotNull();
        assertThat(parsedToken.expiresAt()).isNotNull();
    }

    @Test
    void ValidateToken_InvalidToken_ReturnsNull() {
        JwtTokenService service = new JwtTokenService(VALID_SECRET, CLOCK);
        service.init();

        ParsedToken parsedToken = service.validateToken("invalid.token.value");

        assertThat(parsedToken).isNull();
    }

    @Test
    void ValidateToken_TamperedToken_ReturnsNull() {
        JwtTokenService service = new JwtTokenService(VALID_SECRET, CLOCK);
        service.init();
        TokenExternalInfo tokenInfo = service.createToken(123L);
        String tamperedToken = tokenInfo.tokenValue() + "tampered";

        ParsedToken parsedToken = service.validateToken(tamperedToken);

        assertThat(parsedToken).isNull();
    }

    @Test
    void ValidateToken_NonNumericSubject_ReturnsNull() {
        JwtTokenService service = new JwtTokenService(VALID_SECRET, CLOCK);
        service.init();
        String token = Jwts.builder()
                .subject("not-a-number")
                .id(UUID.randomUUID().toString())
                .expiration(new Date(CLOCK.millis() + 10000))
                .signWith(Keys.hmacShaKeyFor(VALID_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        ParsedToken parsedToken = service.validateToken(token);

        assertThat(parsedToken).isNull();
    }

    @Test
    void ValidateToken_MissingJti_ReturnsNull() {
        JwtTokenService service = new JwtTokenService(VALID_SECRET, CLOCK);
        service.init();
        String token = Jwts.builder()
                .subject("123")
                .expiration(new Date(CLOCK.millis() + 10000))
                .signWith(Keys.hmacShaKeyFor(VALID_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        ParsedToken parsedToken = service.validateToken(token);

        assertThat(parsedToken).isNull();
    }

    @Test
    void ValidateToken_MissingSubject_ReturnsNull() {
        JwtTokenService service = new JwtTokenService(VALID_SECRET, CLOCK);
        service.init();
        String token = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .expiration(new Date(CLOCK.millis() + 10000))
                .signWith(Keys.hmacShaKeyFor(VALID_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        ParsedToken parsedToken = service.validateToken(token);

        assertThat(parsedToken).isNull();
    }

    @Test
    void ValidateToken_ExpiredToken_ReturnsNull() {
        JwtTokenService service = new JwtTokenService(VALID_SECRET, CLOCK);
        service.init();
        String expiredToken = Jwts.builder()
                .subject("123")
                .id(UUID.randomUUID().toString())
                .expiration(new Date(CLOCK.millis() - 1000))
                .signWith(Keys.hmacShaKeyFor(VALID_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        ParsedToken parsedToken = service.validateToken(expiredToken);

        assertThat(parsedToken).isNull();
    }
}