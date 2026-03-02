package pt.isel.pipeline.authentication;

import org.springframework.stereotype.Component;
import pt.isel.domain.users.AuthenticatedUser;
import pt.isel.services.users.UserService;

@Component
public class RequestTokenProcessor {
    public static final String SCHEME = "bearer";
    private final UserService usersService;

    public RequestTokenProcessor(UserService usersService) {
        this.usersService = usersService;
    }

    public AuthenticatedUser processAuthorizationHeaderValue(String authorizationValue) {
        if (authorizationValue == null || authorizationValue.isBlank()) return null;

        String[] parts = authorizationValue.trim().split("\\s+");
        if (parts.length != 2 || !parts[0].equalsIgnoreCase(SCHEME)) return null;

        String token = parts[1];
        var user = usersService.getUserByToken(token);
        return user != null ? new AuthenticatedUser(user, token) : null;
    }
}