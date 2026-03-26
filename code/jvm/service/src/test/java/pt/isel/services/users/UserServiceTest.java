package pt.isel.services.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.common.Either;
import pt.isel.domain.common.UserError;
import pt.isel.domain.invitations.Invitation;
import pt.isel.domain.security.PasswordEncoder;
import pt.isel.domain.security.PasswordPolicyConfig;
import pt.isel.domain.security.PasswordSecurityDomain;
import pt.isel.domain.security.TokenExternalInfo;
import pt.isel.domain.users.User;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.mem.TransactionManagerInMem;
import pt.isel.services.common.RateLimiter;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserServiceTest {

    private TransactionManagerInMem trxManager;
    private UserService userService;
    private boolean rateLimitTriggered = false;
    private Clock clock;

    @BeforeEach
    void setUp() {
        trxManager = new TransactionManagerInMem();
        rateLimitTriggered = false;

        PasswordEncoder encoder = new PasswordEncoder() {
            @Override
            public String encode(String rawPassword) {
                return "encoded_" + rawPassword;
            }

            @Override
            public boolean matches(String rawPassword, String encodedPassword) {
                return encodedPassword.equals("encoded_" + rawPassword);
            }
        };

        PasswordPolicyConfig config = new PasswordPolicyConfig(8, true, true, true, true);
        PasswordSecurityDomain securityDomain = new PasswordSecurityDomain(encoder, config);

        TokenService tokenService = new TokenService() {
            @Override
            public TokenExternalInfo createToken(Long userId) {
                return new TokenExternalInfo("token-" + userId, Instant.now().plusSeconds(3600), userId);
            }

            @Override
            public ParsedToken validateToken(String token) {
                if (token.startsWith("token-")) {
                    Long id = Long.parseLong(token.substring(6));
                    return new ParsedToken("jti-" + id, id, LocalDateTime.now().plusHours(1));
                }
                return null;
            }
        };

        RateLimiter rateLimiter = (action, identifier, limit, window) -> rateLimitTriggered;
        clock = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneOffset.UTC);

        userService = new UserService(trxManager, securityDomain, tokenService, clock, rateLimiter);
    }

    @Test
    void testRegisterUser_Success() {
        Either<UserError, TokenExternalInfo> result = userService.registerUser("alice", "Strong1!", null);

        assertInstanceOf(Either.Right.class, result);
        TokenExternalInfo tokenInfo = ((Either.Right<UserError, TokenExternalInfo>) result).value();
        assertNotNull(tokenInfo.tokenValue());
        assertEquals(1L, tokenInfo.userId());
    }

    @Test
    void testRegisterUser_WithInvitation() {
        User owner = ((Either.Right<UserError, User>) userService.getUserById(
                ((Either.Right<UserError, TokenExternalInfo>) userService.registerUser("owner", "Strong1!", null)).value().userId()
        )).value();

        Channel channel = trxManager.run(trx -> trx.repoChannels().create("Secret", new UserInfo(owner.id(), owner.username()), false));
        Invitation inv = trxManager.run(trx -> trx.repoInvitations().create(
                "inv-token", new UserInfo(owner.id(), owner.username()), channel, AccessType.READ_WRITE, LocalDateTime.now(clock).plusDays(1)
        ));

        Either<UserError, TokenExternalInfo> result = userService.registerUser("bob", "Strong1!", inv.token());

        assertInstanceOf(Either.Right.class, result);
        Long bobId = ((Either.Right<UserError, TokenExternalInfo>) result).value().userId();

        trxManager.run(trx -> {
            assertNotNull(trx.repoMemberships().findUserInChannel(bobId, channel.id()));
            return null;
        });
    }

    @Test
    void testRegisterUser_InvitationExpired() {
        User owner = ((Either.Right<UserError, User>) userService.getUserById(
                ((Either.Right<UserError, TokenExternalInfo>) userService.registerUser("owner", "Strong1!", null)).value().userId()
        )).value();

        Channel channel = trxManager.run(trx -> trx.repoChannels().create("Secret", new UserInfo(owner.id(), owner.username()), false));
        Invitation inv = trxManager.run(trx -> trx.repoInvitations().create(
                "expired-inv-token", new UserInfo(owner.id(), owner.username()), channel, AccessType.READ_WRITE, LocalDateTime.now(clock).minusDays(1)
        ));

        Either<UserError, TokenExternalInfo> result = userService.registerUser("bob", "Strong1!", inv.token());

        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(UserError.InvitationExpired.class, ((Either.Left<UserError, TokenExternalInfo>) result).value());
    }

    @Test
    void testRegisterUser_InsecurePassword() {
        Either<UserError, TokenExternalInfo> result = userService.registerUser("alice", "weak", null);

        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(UserError.InsecurePassword.class, ((Either.Left<UserError, TokenExternalInfo>) result).value());
    }

    @Test
    void testRegisterUser_UsernameAlreadyInUse() {
        userService.registerUser("alice", "Strong1!", null);
        Either<UserError, TokenExternalInfo> result = userService.registerUser("alice", "Strong2!", null);

        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(UserError.UsernameAlreadyInUse.class, ((Either.Left<UserError, TokenExternalInfo>) result).value());
    }

    @Test
    void testGetUserById_Success() {
        Long id = ((Either.Right<UserError, TokenExternalInfo>) userService.registerUser("alice", "Strong1!", null)).value().userId();
        Either<UserError, User> result = userService.getUserById(id);

        assertInstanceOf(Either.Right.class, result);
        assertEquals("alice", ((Either.Right<UserError, User>) result).value().username());
    }

    @Test
    void testUpdateUsername_Success() {
        Long id = ((Either.Right<UserError, TokenExternalInfo>) userService.registerUser("alice", "Strong1!", null)).value().userId();
        Either<UserError, User> result = userService.updateUsername(id, "alice_new", "Strong1!");

        assertInstanceOf(Either.Right.class, result);
        assertEquals("alice_new", ((Either.Right<UserError, User>) result).value().username());
    }

    @Test
    void testUpdateUsername_UsernameAlreadyInUse() {
        Long id1 = ((Either.Right<UserError, TokenExternalInfo>) userService.registerUser("alice", "Strong1!", null)).value().userId();
        userService.registerUser("bob", "Strong1!", null);

        Either<UserError, User> result = userService.updateUsername(id1, "bob", "Strong1!");

        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(UserError.UsernameAlreadyInUse.class, ((Either.Left<UserError, User>) result).value());
    }

    @Test
    void testUpdateUsername_IncorrectPassword() {
        Long id = ((Either.Right<UserError, TokenExternalInfo>) userService.registerUser("alice", "Strong1!", null)).value().userId();
        Either<UserError, User> result = userService.updateUsername(id, "alice_new", "Wrong1!");

        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(UserError.IncorrectPassword.class, ((Either.Left<UserError, User>) result).value());
    }

    @Test
    void testUpdatePassword_Success() {
        Long id = ((Either.Right<UserError, TokenExternalInfo>) userService.registerUser("alice", "Strong1!", null)).value().userId();
        Either<UserError, User> result = userService.updatePassword(id, "Strong1!", "Stronger2@");

        assertInstanceOf(Either.Right.class, result);
    }

    @Test
    void testUpdatePassword_SameAsPrevious() {
        Long id = ((Either.Right<UserError, TokenExternalInfo>) userService.registerUser("alice", "Strong1!", null)).value().userId();
        Either<UserError, User> result = userService.updatePassword(id, "Strong1!", "Strong1!");

        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(UserError.PasswordSameAsPrevious.class, ((Either.Left<UserError, User>) result).value());
    }

    @Test
    void testDeleteUser_Success() {
        Long id = ((Either.Right<UserError, TokenExternalInfo>) userService.registerUser("alice", "Strong1!", null)).value().userId();
        Either<UserError, String> result = userService.deleteUser(id);

        assertInstanceOf(Either.Right.class, result);
        assertInstanceOf(Either.Left.class, userService.getUserById(id));
    }

    @Test
    void testDeleteUser_HasOwnedChannels() {
        Long id = ((Either.Right<UserError, TokenExternalInfo>) userService.registerUser("alice", "Strong1!", null)).value().userId();
        trxManager.run(trx -> trx.repoChannels().create("General", new UserInfo(id, "alice"), true));

        Either<UserError, String> result = userService.deleteUser(id);

        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(UserError.UserHasOwnedChannels.class, ((Either.Left<UserError, String>) result).value());
    }

    @Test
    void testCreateToken_Success() {
        userService.registerUser("alice", "Strong1!", null);
        Either<UserError, TokenExternalInfo> result = userService.createToken("alice", "Strong1!");

        assertInstanceOf(Either.Right.class, result);
        assertNotNull(((Either.Right<UserError, TokenExternalInfo>) result).value().tokenValue());
    }

    @Test
    void testCreateToken_RateLimited() {
        userService.registerUser("alice", "Strong1!", null);
        rateLimitTriggered = true;
        Either<UserError, TokenExternalInfo> result = userService.createToken("alice", "Strong1!");

        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(UserError.RateLimitExceeded.class, ((Either.Left<UserError, TokenExternalInfo>) result).value());
    }

    @Test
    void testGetUserByToken_Success() {
        Long id = ((Either.Right<UserError, TokenExternalInfo>) userService.registerUser("alice", "Strong1!", null)).value().userId();
        User user = userService.getUserByToken("token-" + id);

        assertNotNull(user);
        assertEquals(id, user.id());
    }

    @Test
    void testRevokeToken_Success() {
        Long id = ((Either.Right<UserError, TokenExternalInfo>) userService.registerUser("alice", "Strong1!", null)).value().userId();
        String token = "token-" + id;

        assertNotNull(userService.getUserByToken(token));
        userService.revokeToken(token);
        assertNull(userService.getUserByToken(token));
    }

    @Test
    void testRegisterUser_EmptyUsername() {
        assertInstanceOf(Either.Left.class, userService.registerUser("", "Strong1!", null));
        assertInstanceOf(Either.Left.class, userService.registerUser(null, "Strong1!", null));
    }

    @Test
    void testRegisterUser_InvalidUsernameLength() {
        String longName = "a".repeat(31);
        Either<UserError, TokenExternalInfo> result = userService.registerUser(longName, "Strong1!", null);
        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(UserError.InvalidUsernameLength.class, ((Either.Left<UserError, TokenExternalInfo>) result).value());
    }

    @Test
    void testRegisterUser_EmptyPassword() {
        assertInstanceOf(Either.Left.class, userService.registerUser("bob", "", null));
        assertInstanceOf(Either.Left.class, userService.registerUser("bob", null, null));
    }

    @Test
    void testRegisterUser_InvitationNotFound() {
        Either<UserError, TokenExternalInfo> result = userService.registerUser("bob", "Strong1!", "invalid-token");
        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(UserError.InvitationNotFound.class, ((Either.Left<UserError, TokenExternalInfo>) result).value());
    }

    @Test
    void testGetUserById_NotFound() {
        Either<UserError, User> result = userService.getUserById(999L);
        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(UserError.UserNotFound.class, ((Either.Left<UserError, User>) result).value());
    }

    @Test
    void testUpdateUsername_EmptyUsername() {
        Long id = ((Either.Right<UserError, TokenExternalInfo>) userService.registerUser("alice", "Strong1!", null)).value().userId();
        assertInstanceOf(Either.Left.class, userService.updateUsername(id, "", "Strong1!"));
    }

    @Test
    void testUpdateUsername_UserNotFound() {
        Either<UserError, User> result = userService.updateUsername(999L, "new_name", "Strong1!");
        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(UserError.UserNotFound.class, ((Either.Left<UserError, User>) result).value());
    }

    @Test
    void testUpdatePassword_EmptyPassword() {
        Long id = ((Either.Right<UserError, TokenExternalInfo>) userService.registerUser("alice", "Strong1!", null)).value().userId();
        assertInstanceOf(Either.Left.class, userService.updatePassword(id, "Strong1!", ""));
    }

    @Test
    void testUpdatePassword_InsecurePassword() {
        Long id = ((Either.Right<UserError, TokenExternalInfo>) userService.registerUser("alice", "Strong1!", null)).value().userId();
        Either<UserError, User> result = userService.updatePassword(id, "Strong1!", "weak");
        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(UserError.InsecurePassword.class, ((Either.Left<UserError, User>) result).value());
    }

    @Test
    void testUpdatePassword_IncorrectOldPassword() {
        Long id = ((Either.Right<UserError, TokenExternalInfo>) userService.registerUser("alice", "Strong1!", null)).value().userId();
        Either<UserError, User> result = userService.updatePassword(id, "Wrong1!", "Stronger2@");
        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(UserError.IncorrectPassword.class, ((Either.Left<UserError, User>) result).value());
    }

    @Test
    void testDeleteUser_NotFound() {
        Either<UserError, String> result = userService.deleteUser(999L);
        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(UserError.UserNotFound.class, ((Either.Left<UserError, String>) result).value());
    }

    @Test
    void testCreateToken_EmptyCredentials() {
        assertInstanceOf(Either.Left.class, userService.createToken("", "Strong1!"));
        assertInstanceOf(Either.Left.class, userService.createToken("alice", ""));
    }

    @Test
    void testCreateToken_UserNotFound() {
        Either<UserError, TokenExternalInfo> result = userService.createToken("ghost", "Strong1!");
        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(UserError.UserNotFound.class, ((Either.Left<UserError, TokenExternalInfo>) result).value());
    }

    @Test
    void testCreateToken_IncorrectPassword() {
        userService.registerUser("alice", "Strong1!", null);
        Either<UserError, TokenExternalInfo> result = userService.createToken("alice", "Wrong1!");
        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(UserError.IncorrectPassword.class, ((Either.Left<UserError, TokenExternalInfo>) result).value());
    }

    @Test
    void testGetUserByToken_InvalidToken() {
        assertNull(userService.getUserByToken("invalid-format"));
    }

    @Test
    void testGetUserByToken_BlacklistedToken() {
        Long id = ((Either.Right<UserError, TokenExternalInfo>) userService.registerUser("alice", "Strong1!", null)).value().userId();
        String token = "token-" + id;

        userService.revokeToken(token);
        assertNull(userService.getUserByToken(token));
    }

    @Test
    void testRegisterUser_InvitationAlreadyUsed() {
        User owner = ((Either.Right<UserError, User>) userService.getUserById(
                ((Either.Right<UserError, TokenExternalInfo>) userService.registerUser("owner", "Strong1!", null)).value().userId()
        )).value();

        Channel channel = trxManager.run(trx -> trx.repoChannels().create("Secret", new UserInfo(owner.id(), owner.username()), false));
        trxManager.run(trx -> trx.repoInvitations().create(
                "inv-token", new UserInfo(owner.id(), owner.username()), channel, AccessType.READ_WRITE, LocalDateTime.now(clock).plusDays(1)
        ));

        userService.registerUser("bob", "Strong1!", "inv-token");
        Either<UserError, TokenExternalInfo> result = userService.registerUser("charlie", "Strong1!", "inv-token");

        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(UserError.InvitationAlreadyUsed.class, ((Either.Left<UserError, TokenExternalInfo>) result).value());
    }

    @Test
    void testUpdateUsername_InvalidUsernameLength() {
        Long id = ((Either.Right<UserError, TokenExternalInfo>) userService.registerUser("alice", "Strong1!", null)).value().userId();
        String longName = "a".repeat(31);

        Either<UserError, User> result = userService.updateUsername(id, longName, "Strong1!");

        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(UserError.InvalidUsernameLength.class, ((Either.Left<UserError, User>) result).value());
    }

    @Test
    void testUpdatePassword_UserNotFound() {
        Either<UserError, User> result = userService.updatePassword(999L, "Strong1!", "Stronger2@");

        assertInstanceOf(Either.Left.class, result);
        assertInstanceOf(UserError.UserNotFound.class, ((Either.Left<UserError, User>) result).value());
    }

    @Test
    void testRevokeToken_InvalidToken() {
        assertDoesNotThrow(() -> userService.revokeToken("invalid-token-format"));
    }

    @Test
    void testCleanupExpiredTokens() {
        assertDoesNotThrow(() -> userService.cleanupExpiredTokens());
    }
}