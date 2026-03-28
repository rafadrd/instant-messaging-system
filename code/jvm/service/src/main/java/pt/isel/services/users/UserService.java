package pt.isel.services.users;

import jakarta.inject.Named;
import pt.isel.domain.common.Either;
import pt.isel.domain.common.UserError;
import pt.isel.domain.invitations.Invitation;
import pt.isel.domain.invitations.InvitationStatus;
import pt.isel.domain.security.PasswordSecurityDomain;
import pt.isel.domain.security.TokenExternalInfo;
import pt.isel.domain.users.User;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.Transaction;
import pt.isel.repositories.TransactionManager;
import pt.isel.services.common.RateLimiter;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

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

    public Either<UserError, TokenExternalInfo> registerUser(String username, String password, String token) {
        return trxManager.run(trx -> {
            Invitation invitation;

            if (token != null && !token.isBlank()) {
                invitation = trx.repoInvitations().findByToken(token);
                if (invitation == null) return Either.failure(new UserError.InvitationNotFound());

                if (invitation.expiresAt().isBefore(LocalDateTime.now(clock))) {
                    return Either.failure(new UserError.InvitationExpired());
                }
                if (invitation.status() != InvitationStatus.PENDING) {
                    return Either.failure(new UserError.InvitationAlreadyUsed());
                }
            } else {
                invitation = null;
            }

            return createUser(trx, username, password).flatMap(user -> {
                if (invitation != null) {
                    if (!trx.repoInvitations().consume(invitation.id())) {
                        return Either.failure(new UserError.InvitationAlreadyUsed());
                    }
                    trx.repoMemberships().addUserToChannel(
                            new UserInfo(user.id(), user.username()),
                            invitation.channel(),
                            invitation.accessType()
                    );
                }
                return Either.success(tokenService.createToken(user.id()));
            });
        });
    }

    private Either<UserError, User> createUser(Transaction trx, String username, String password) {
        if (username == null || username.isBlank()) return Either.failure(new UserError.EmptyUsername());
        if (username.length() > 30) return Either.failure(new UserError.InvalidUsernameLength());
        if (password == null || password.isBlank()) return Either.failure(new UserError.EmptyPassword());

        if (!passwordSecurityDomain.isSafePassword(password)) {
            return Either.failure(new UserError.InsecurePassword());
        }

        var validationInfo = passwordSecurityDomain.createPasswordValidationInformation(password);

        try {
            User newUser = trx.repoUsers().create(username, validationInfo);
            return Either.success(newUser);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("users_username_key")) {
                return Either.failure(new UserError.UsernameAlreadyInUse());
            }
            throw e;
        }
    }

    public Either<UserError, User> getUserById(Long userId) {
        return trxManager.run(trx -> {
            User user = trx.repoUsers().findById(userId);
            return user != null ? Either.success(user) : Either.failure(new UserError.UserNotFound());
        });
    }

    public Either<UserError, User> updateUsername(Long userId, String newUsername, String password) {
        if (newUsername == null || newUsername.isBlank()) return Either.failure(new UserError.EmptyUsername());
        if (newUsername.length() > 30) return Either.failure(new UserError.InvalidUsernameLength());

        return trxManager.run(trx -> {
            User user = trx.repoUsers().findById(userId);
            if (user == null) return Either.failure(new UserError.UserNotFound());

            if (!passwordSecurityDomain.validatePassword(password, user.passwordValidation())) {
                return Either.failure(new UserError.IncorrectPassword());
            }

            try {
                User updatedUser = new User(user.id(), newUsername, user.passwordValidation());
                trx.repoUsers().save(updatedUser);
                return Either.success(updatedUser);
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("users_username_key")) {
                    return Either.failure(new UserError.UsernameAlreadyInUse());
                }
                throw e;
            }
        });
    }

    public Either<UserError, User> updatePassword(Long userId, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) return Either.failure(new UserError.EmptyPassword());
        if (!passwordSecurityDomain.isSafePassword(newPassword))
            return Either.failure(new UserError.InsecurePassword());

        return trxManager.run(trx -> {
            User user = trx.repoUsers().findById(userId);
            if (user == null) return Either.failure(new UserError.UserNotFound());

            if (!passwordSecurityDomain.validatePassword(oldPassword, user.passwordValidation())) {
                return Either.failure(new UserError.IncorrectPassword());
            }

            if (passwordSecurityDomain.validatePassword(newPassword, user.passwordValidation())) {
                return Either.failure(new UserError.PasswordSameAsPrevious());
            }

            var newValidation = passwordSecurityDomain.createPasswordValidationInformation(newPassword);
            User updatedUser = new User(user.id(), user.username(), newValidation);
            trx.repoUsers().save(updatedUser);
            return Either.success(updatedUser);
        });
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
            trx.repoTokenBlacklist().cleanupExpired(LocalDateTime.now(clock));
            return null;
        });
    }
}