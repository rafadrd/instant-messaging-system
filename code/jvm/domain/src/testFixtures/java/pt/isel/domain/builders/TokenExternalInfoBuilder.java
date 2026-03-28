package pt.isel.domain.builders;

import pt.isel.domain.security.TokenExternalInfo;

import java.time.Instant;

public class TokenExternalInfoBuilder {
    private String tokenValue = "token123";
    private Instant tokenExpiration = Instant.now().plusSeconds(3600);
    private Long userId = 1L;

    public TokenExternalInfoBuilder withTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
        return this;
    }

    public TokenExternalInfoBuilder withTokenExpiration(Instant tokenExpiration) {
        this.tokenExpiration = tokenExpiration;
        return this;
    }

    public TokenExternalInfoBuilder withUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    public TokenExternalInfo build() {
        return new TokenExternalInfo(tokenValue, tokenExpiration, userId);
    }
}