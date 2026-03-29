package pt.isel.services.fakes;

import pt.isel.domain.builders.TokenExternalInfoBuilder;
import pt.isel.domain.security.TokenExternalInfo;
import pt.isel.services.builders.ParsedTokenBuilder;
import pt.isel.services.users.ParsedToken;
import pt.isel.services.users.TokenService;

public class FakeTokenService implements TokenService {
    @Override
    public TokenExternalInfo createToken(Long userId) {
        return new TokenExternalInfoBuilder().withTokenValue("token-" + userId).withUserId(userId).build();
    }

    @Override
    public ParsedToken validateToken(String token) {
        if (token != null && token.startsWith("token-")) {
            try {
                Long id = Long.parseLong(token.substring(6));
                return new ParsedTokenBuilder().withJti("jti-" + id).withUserId(id).build();
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}