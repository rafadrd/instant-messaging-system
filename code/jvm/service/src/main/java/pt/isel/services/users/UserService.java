package pt.isel.services.users;

import jakarta.inject.Named;
import pt.isel.domain.common.Either;
import pt.isel.domain.common.UserError;
import pt.isel.domain.security.PasswordSecurityDomain;
import pt.isel.domain.security.TokenExternalInfo;
import pt.isel.domain.users.User;
import pt.isel.repositories.TransactionManager;
import pt.isel.services.common.RateLimiter;

import java.time.Clock;
import java.time.Duration;

@Named
public class UserService {
    private final TransactionManager trxManager;
    private final PasswordSecurityDomain passwordSecurityDomain;
    private final TokenService tokenService;
    private final Clock clock;
    private final RateLimiter rateLimiter;

    public UserService(TransactionManager trxManager, PasswordSecurityDomain passwordSecurityDomain, TokenService tokenService, Clock clock, RateLimiter rateLimiter) {
        this.trxManager = trxManager;
        this.passwordSecurityDomain = passwordSecurityDomain;
        this.tokenService = tokenService;
        this.clock = clock;
        this.rateLimiter = rateLimiter;
    }

    public Either<UserError, String> deleteUser(Long userId) {
        return trxManager.run(trx -> {
            User user = trx.repoUsers().findById(userId);
            if (user == null) return Either.failure(new UserError.UserNotFound());

            if (!trx.repoChannels().findAllByOwner(user.id()).isEmpty()) {
                return Either.failure(new UserError.UserHasOwnedChannels());
            }

            trx.repoUsers().deleteById(userId);
            return Either.success("User " + user.id() + " deleted");
        });
    }

    public Either<UserError, TokenExternalInfo> createToken(String username, String password) {
        if (rateLimiter.isRateLimited("login", username, 5, Duration.ofMinutes(1))) {
            return Either.failure(new UserError.RateLimitExceeded());
        }

        if (username == null || username.isBlank()) return Either.failure(new UserError.EmptyUsername());
        if (password == null || password.isBlank()) return Either.failure(new UserError.EmptyPassword());

        return trxManager.run(trx -> {
            User user = trx.repoUsers().findByUsername(username);
            if (user == null) return Either.failure(new UserError.UserNotFound());

            if (!passwordSecurityDomain.validatePassword(password, user.passwordValidation())) {
                return Either.failure(new UserError.IncorrectPassword());
            }

            return Either.success(tokenService.createToken(user.id()));
        });
    }

    public User getUserByToken(String token) {
        ParsedToken parsedToken = tokenService.validateToken(token);
        if (parsedToken == null) return null;

        return trxManager.run(trx -> {
            if (trx.repoTokenBlacklist().exists(parsedToken.jti())) {
                return null;
            }
            return trx.repoUsers().findById(parsedToken.userId());
        });
    }

    public void revokeToken(String token) {
        ParsedToken parsedToken = tokenService.validateToken(token);
        if (parsedToken == null) return;
        trxManager.run(trx -> {
            trx.repoTokenBlacklist().add(parsedToken.jti(), parsedToken.expiresAt());
            return null;
        });
    }

    public void cleanupExpiredTokens() {
        trxManager.run(trx -> {
            trx.repoTokenBlacklist().cleanupExpired();
            return null;
        });
    }
}