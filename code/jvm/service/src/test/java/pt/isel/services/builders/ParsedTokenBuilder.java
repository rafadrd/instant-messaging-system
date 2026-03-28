package pt.isel.services.builders;

import pt.isel.services.users.ParsedToken;

import java.time.LocalDateTime;

public class ParsedTokenBuilder {
    private String jti = "jti-123";
    private Long userId = 1L;
    private LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

    public ParsedTokenBuilder withJti(String jti) {
        this.jti = jti;
        return this;
    }

    public ParsedTokenBuilder withUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    public ParsedTokenBuilder withExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public ParsedToken build() {
        return new ParsedToken(jti, userId, expiresAt);
    }
}