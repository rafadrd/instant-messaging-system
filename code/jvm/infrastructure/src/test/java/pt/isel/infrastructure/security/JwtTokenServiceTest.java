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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        assertThrows(WeakKeyException.class, service::init);
    }

    @Test
    void testCreateAndValidateTokenSuccess() {
        JwtTokenService service = new JwtTokenService(VALID_SECRET);
        service.init();

        Long userId = 123L;
        TokenExternalInfo tokenInfo = service.createToken(userId);

        assertNotNull(tokenInfo);
        assertNotNull(tokenInfo.tokenValue());
        assertEquals(userId, tokenInfo.userId());
        assertNotNull(tokenInfo.tokenExpiration());

        ParsedToken parsedToken = service.validateToken(tokenInfo.tokenValue());

        assertNotNull(parsedToken);
        assertEquals(userId, parsedToken.userId());
        assertNotNull(parsedToken.jti());
        assertNotNull(parsedToken.expiresAt());
    }

    @Test
    void testValidateTokenReturnsNullForInvalidToken() {
        JwtTokenService service = new JwtTokenService(VALID_SECRET);
        service.init();

        ParsedToken parsedToken = service.validateToken("invalid.token.value");
        assertNull(parsedToken);
    }

    @Test
    void testValidateTokenReturnsNullForTamperedToken() {
        JwtTokenService service = new JwtTokenService(VALID_SECRET);
        service.init();

        TokenExternalInfo tokenInfo = service.createToken(123L);
        String tamperedToken = tokenInfo.tokenValue() + "tampered";

        ParsedToken parsedToken = service.validateToken(tamperedToken);
        assertNull(parsedToken);
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
        assertNull(parsedToken);
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
        assertNull(parsedToken);
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
        assertNull(parsedToken);
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
        assertNull(parsedToken);
    }
}