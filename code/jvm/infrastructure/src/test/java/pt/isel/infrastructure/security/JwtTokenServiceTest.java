package pt.isel.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;
import pt.isel.domain.security.TokenExternalInfo;
import pt.isel.services.users.ParsedToken;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private static final String VALID_SECRET;
    private static final String INVALID_SECRET = "short-secret";

    static {
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        VALID_SECRET = Base64.getEncoder().encodeToString(keyBytes);
    }

    @Test
    void testInitThrowsWeakKeyExceptionForShortSecret() {
        JwtTokenService service = new JwtTokenService(INVALID_SECRET);
        assertThatThrownBy(service::init).isInstanceOf(WeakKeyException.class);
    }

    @Test
    void testCreateAndValidateTokenSuccess() {
        JwtTokenService service = new JwtTokenService(VALID_SECRET);
        service.init();

        Long userId = 123L;
        TokenExternalInfo tokenInfo = service.createToken(userId);

        assertThat(tokenInfo).isNotNull();
        assertThat(tokenInfo.tokenValue()).isNotNull();
        assertThat(tokenInfo.userId()).isEqualTo(userId);
        assertThat(tokenInfo.tokenExpiration()).isNotNull();

        ParsedToken parsedToken = service.validateToken(tokenInfo.tokenValue());

        assertThat(parsedToken).isNotNull();
        assertThat(parsedToken.userId()).isEqualTo(userId);
        assertThat(parsedToken.jti()).isNotNull();
        assertThat(parsedToken.expiresAt()).isNotNull();
    }

    @Test
    void testValidateTokenReturnsNullForInvalidToken() {
        JwtTokenService service = new JwtTokenService(VALID_SECRET);
        service.init();

        ParsedToken parsedToken = service.validateToken("invalid.token.value");
        assertThat(parsedToken).isNull();
    }

    @Test
    void testValidateTokenReturnsNullForTamperedToken() {
        JwtTokenService service = new JwtTokenService(VALID_SECRET);
        service.init();

        TokenExternalInfo tokenInfo = service.createToken(123L);
        String tamperedToken = tokenInfo.tokenValue() + "tampered";

        ParsedToken parsedToken = service.validateToken(tamperedToken);
        assertThat(parsedToken).isNull();
    }

    @Test
    void testValidateTokenWithNonNumericSubject() {
        JwtTokenService service = new JwtTokenService(VALID_SECRET);
        service.init();

        String token = Jwts.builder()
                .subject("not-a-number")
                .id(UUID.randomUUID().toString())
                .expiration(new Date(System.currentTimeMillis() + 10000))
                .signWith(Keys.hmacShaKeyFor(VALID_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        ParsedToken parsedToken = service.validateToken(token);
        assertThat(parsedToken).isNull();
    }

    @Test
    void testValidateTokenMissingJti() {
        JwtTokenService service = new JwtTokenService(VALID_SECRET);
        service.init();

        String token = Jwts.builder()
                .subject("123")
                .expiration(new Date(System.currentTimeMillis() + 10000))
                .signWith(Keys.hmacShaKeyFor(VALID_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        ParsedToken parsedToken = service.validateToken(token);
        assertThat(parsedToken).isNull();
    }

    @Test
    void testValidateTokenMissingSubject() {
        JwtTokenService service = new JwtTokenService(VALID_SECRET);
        service.init();

        String token = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .expiration(new Date(System.currentTimeMillis() + 10000))
                .signWith(Keys.hmacShaKeyFor(VALID_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        ParsedToken parsedToken = service.validateToken(token);
        assertThat(parsedToken).isNull();
    }

    @Test
    void testValidateTokenExpired() {
        JwtTokenService service = new JwtTokenService(VALID_SECRET);
        service.init();

        String expiredToken = Jwts.builder()
                .subject("123")
                .id(UUID.randomUUID().toString())
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(Keys.hmacShaKeyFor(VALID_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        ParsedToken parsedToken = service.validateToken(expiredToken);
        assertThat(parsedToken).isNull();
    }
}