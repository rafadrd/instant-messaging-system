package pt.isel.services.users;

import pt.isel.domain.security.TokenExternalInfo;

public interface TokenService {
    TokenExternalInfo createToken(Long userId);

    ParsedToken validateToken(String token);
}