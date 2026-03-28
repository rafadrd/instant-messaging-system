package pt.isel.services.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import pt.isel.domain.builders.UserInfoBuilder;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.common.Either;
import pt.isel.domain.common.EitherAssert;
import pt.isel.domain.common.UserError;
import pt.isel.domain.invitations.Invitation;
import pt.isel.domain.security.PasswordEncoder;
import pt.isel.domain.security.PasswordPolicyConfig;
import pt.isel.domain.security.PasswordSecurityDomain;
import pt.isel.domain.security.TokenExternalInfo;
import pt.isel.domain.users.User;
import pt.isel.repositories.mem.TransactionManagerInMem;
import pt.isel.services.common.RateLimiter;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

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

        TokenExternalInfo tokenInfo = EitherAssert.assertRight(result);
        assertThat(tokenInfo.tokenValue()).isNotNull();
        assertThat(tokenInfo.userId()).isEqualTo(1L);
    }

    @Test
    void testRegisterUser_WithInvitation() {
        User owner = EitherAssert.assertRight(userService.getUserById(
                EitherAssert.assertRight(userService.registerUser("owner", "Strong1!", null)).userId()
        ));

        Channel channel = trxManager.run(trx -> trx.repoChannels().create("Secret", new UserInfoBuilder().withId(owner.id()).withUsername(owner.username()).build(), false));
        Invitation inv = trxManager.run(trx -> trx.repoInvitations().create(
                "inv-token", new UserInfoBuilder().withId(owner.id()).withUsername(owner.username()).build(), channel, AccessType.READ_WRITE, LocalDateTime.now(clock).plusDays(1)
        ));

        Either<UserError, TokenExternalInfo> result = userService.registerUser("bob", "Strong1!", inv.token());

        Long bobId = EitherAssert.assertRight(result).userId();

        trxManager.run(trx -> {
            assertThat(trx.repoMemberships().findUserInChannel(bobId, channel.id())).isNotNull();
            return null;
        });
    }

    @Test
    void testRegisterUser_InvitationExpired() {
        User owner = EitherAssert.assertRight(userService.getUserById(
                EitherAssert.assertRight(userService.registerUser("owner", "Strong1!", null)).userId()
        ));

        Channel channel = trxManager.run(trx -> trx.repoChannels().create("Secret", new UserInfoBuilder().withId(owner.id()).withUsername(owner.username()).build(), false));
        Invitation inv = trxManager.run(trx -> trx.repoInvitations().create(
                "expired-inv-token", new UserInfoBuilder().withId(owner.id()).withUsername(owner.username()).build(), channel, AccessType.READ_WRITE, LocalDateTime.now(clock).minusDays(1)
        ));

        Either<UserError, TokenExternalInfo> result = userService.registerUser("bob", "Strong1!", inv.token());

        EitherAssert.assertLeft(result, UserError.InvitationExpired.class);
    }

    @Test
    void testRegisterUser_InsecurePassword() {
        Either<UserError, TokenExternalInfo> result = userService.registerUser("alice", "weak", null);

        EitherAssert.assertLeft(result, UserError.InsecurePassword.class);
    }

    @Test
    void testRegisterUser_UsernameAlreadyInUse() {
        userService.registerUser("alice", "Strong1!", null);
        Either<UserError, TokenExternalInfo> result = userService.registerUser("alice", "Strong2!", null);

        EitherAssert.assertLeft(result, UserError.UsernameAlreadyInUse.class);
    }

    @Test
    void testGetUserById_Success() {
        Long id = EitherAssert.assertRight(userService.registerUser("alice", "Strong1!", null)).userId();
        Either<UserError, User> result = userService.getUserById(id);

        assertThat(EitherAssert.assertRight(result).username()).isEqualTo("alice");
    }

    @Test
    void testUpdateUsername_Success() {
        Long id = EitherAssert.assertRight(userService.registerUser("alice", "Strong1!", null)).userId();
        Either<UserError, User> result = userService.updateUsername(id, "alice_new", "Strong1!");

        assertThat(EitherAssert.assertRight(result).username()).isEqualTo("alice_new");
    }

    @Test
    void testUpdateUsername_UsernameAlreadyInUse() {
        Long id1 = EitherAssert.assertRight(userService.registerUser("alice", "Strong1!", null)).userId();
        userService.registerUser("bob", "Strong1!", null);

        Either<UserError, User> result = userService.updateUsername(id1, "bob", "Strong1!");

        EitherAssert.assertLeft(result, UserError.UsernameAlreadyInUse.class);
    }

    @Test
    void testUpdateUsername_IncorrectPassword() {
        Long id = EitherAssert.assertRight(userService.registerUser("alice", "Strong1!", null)).userId();
        Either<UserError, User> result = userService.updateUsername(id, "alice_new", "Wrong1!");

        EitherAssert.assertLeft(result, UserError.IncorrectPassword.class);
    }

    @Test
    void testUpdatePassword_Success() {
        Long id = EitherAssert.assertRight(userService.registerUser("alice", "Strong1!", null)).userId();
        Either<UserError, User> result = userService.updatePassword(id, "Strong1!", "Stronger2@");

        EitherAssert.assertRight(result);
    }

    @Test
    void testUpdatePassword_SameAsPrevious() {
        Long id = EitherAssert.assertRight(userService.registerUser("alice", "Strong1!", null)).userId();
        Either<UserError, User> result = userService.updatePassword(id, "Strong1!", "Strong1!");

        EitherAssert.assertLeft(result, UserError.PasswordSameAsPrevious.class);
    }

    @Test
    void testDeleteUser_Success() {
        Long id = EitherAssert.assertRight(userService.registerUser("alice", "Strong1!", null)).userId();
        Either<UserError, String> result = userService.deleteUser(id);

        EitherAssert.assertRight(result);
        EitherAssert.assertLeft(userService.getUserById(id));
    }

    @Test
    void testDeleteUser_HasOwnedChannels() {
        Long id = EitherAssert.assertRight(userService.registerUser("alice", "Strong1!", null)).userId();
        trxManager.run(trx -> trx.repoChannels().create("General", new UserInfoBuilder().withId(id).withUsername("alice").build(), true));

        Either<UserError, String> result = userService.deleteUser(id);

        EitherAssert.assertLeft(result, UserError.UserHasOwnedChannels.class);
    }

    @Test
    void testCreateToken_Success() {
        userService.registerUser("alice", "Strong1!", null);
        Either<UserError, TokenExternalInfo> result = userService.createToken("alice", "Strong1!");

        assertThat(EitherAssert.assertRight(result).tokenValue()).isNotNull();
    }

    @Test
    void testCreateToken_RateLimited() {
        userService.registerUser("alice", "Strong1!", null);
        rateLimitTriggered = true;
        Either<UserError, TokenExternalInfo> result = userService.createToken("alice", "Strong1!");

        EitherAssert.assertLeft(result, UserError.RateLimitExceeded.class);
    }

    @Test
    void testGetUserByToken_Success() {
        Long id = EitherAssert.assertRight(userService.registerUser("alice", "Strong1!", null)).userId();
        User user = userService.getUserByToken("token-" + id);

        assertThat(user).isNotNull();
        assertThat(user.id()).isEqualTo(id);
    }

    @Test
    void testRevokeToken_Success() {
        Long id = EitherAssert.assertRight(userService.registerUser("alice", "Strong1!", null)).userId();
        String token = "token-" + id;

        assertThat(userService.getUserByToken(token)).isNotNull();
        userService.revokeToken(token);
        assertThat(userService.getUserByToken(token)).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testRegisterUser_InvalidUsername(String invalidUsername) {
        EitherAssert.assertLeft(userService.registerUser(invalidUsername, "Strong1!", null));
    }

    @Test
    void testRegisterUser_UsernameTooLong() {
        String longName = "a".repeat(31);
        Either<UserError, TokenExternalInfo> result = userService.registerUser(longName, "Strong1!", null);

        EitherAssert.assertLeft(result, UserError.InvalidUsernameLength.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testRegisterUser_InvalidPassword(String invalidPassword) {
        EitherAssert.assertLeft(userService.registerUser("bob", invalidPassword, null));
    }

    @Test
    void testRegisterUser_InvitationNotFound() {
        Either<UserError, TokenExternalInfo> result = userService.registerUser("bob", "Strong1!", "invalid-token");

        EitherAssert.assertLeft(result, UserError.InvitationNotFound.class);
    }

    @Test
    void testGetUserById_NotFound() {
        Either<UserError, User> result = userService.getUserById(999L);

        EitherAssert.assertLeft(result, UserError.UserNotFound.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testUpdateUsername_InvalidUsername(String invalidUsername) {
        Long id = EitherAssert.assertRight(userService.registerUser("alice", "Strong1!", null)).userId();
        EitherAssert.assertLeft(userService.updateUsername(id, invalidUsername, "Strong1!"));
    }

    @Test
    void testUpdateUsername_UsernameTooLong() {
        Long id = EitherAssert.assertRight(userService.registerUser("alice", "Strong1!", null)).userId();
        String longName = "a".repeat(31);

        Either<UserError, User> result = userService.updateUsername(id, longName, "Strong1!");

        EitherAssert.assertLeft(result, UserError.InvalidUsernameLength.class);
    }

    @Test
    void testUpdateUsername_UserNotFound() {
        Either<UserError, User> result = userService.updateUsername(999L, "new_name", "Strong1!");

        EitherAssert.assertLeft(result, UserError.UserNotFound.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testUpdatePassword_InvalidPassword(String invalidPassword) {
        Long id = EitherAssert.assertRight(userService.registerUser("alice", "Strong1!", null)).userId();
        EitherAssert.assertLeft(userService.updatePassword(id, "Strong1!", invalidPassword));
    }

    @Test
    void testUpdatePassword_InsecurePassword() {
        Long id = EitherAssert.assertRight(userService.registerUser("alice", "Strong1!", null)).userId();
        Either<UserError, User> result = userService.updatePassword(id, "Strong1!", "weak");

        EitherAssert.assertLeft(result, UserError.InsecurePassword.class);
    }

    @Test
    void testUpdatePassword_IncorrectOldPassword() {
        Long id = EitherAssert.assertRight(userService.registerUser("alice", "Strong1!", null)).userId();
        Either<UserError, User> result = userService.updatePassword(id, "Wrong1!", "Stronger2@");

        EitherAssert.assertLeft(result, UserError.IncorrectPassword.class);
    }

    @Test
    void testDeleteUser_NotFound() {
        Either<UserError, String> result = userService.deleteUser(999L);

        EitherAssert.assertLeft(result, UserError.UserNotFound.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testCreateToken_InvalidCredentials(String invalidInput) {
        EitherAssert.assertLeft(userService.createToken(invalidInput, "Strong1!"));
        EitherAssert.assertLeft(userService.createToken("alice", invalidInput));
    }

    @Test
    void testCreateToken_UserNotFound() {
        Either<UserError, TokenExternalInfo> result = userService.createToken("ghost", "Strong1!");

        EitherAssert.assertLeft(result, UserError.UserNotFound.class);
    }

    @Test
    void testCreateToken_IncorrectPassword() {
        userService.registerUser("alice", "Strong1!", null);
        Either<UserError, TokenExternalInfo> result = userService.createToken("alice", "Wrong1!");

        EitherAssert.assertLeft(result, UserError.IncorrectPassword.class);
    }

    @Test
    void testGetUserByToken_InvalidToken() {
        assertThat(userService.getUserByToken("invalid-format")).isNull();
    }

    @Test
    void testGetUserByToken_BlacklistedToken() {
        Long id = EitherAssert.assertRight(userService.registerUser("alice", "Strong1!", null)).userId();
        String token = "token-" + id;

        userService.revokeToken(token);
        assertThat(userService.getUserByToken(token)).isNull();
    }

    @Test
    void testRegisterUser_InvitationAlreadyUsed() {
        User owner = EitherAssert.assertRight(userService.getUserById(
                EitherAssert.assertRight(userService.registerUser("owner", "Strong1!", null)).userId()
        ));

        Channel channel = trxManager.run(trx -> trx.repoChannels().create("Secret", new UserInfoBuilder().withId(owner.id()).withUsername(owner.username()).build(), false));
        trxManager.run(trx -> trx.repoInvitations().create(
                "inv-token", new UserInfoBuilder().withId(owner.id()).withUsername(owner.username()).build(), channel, AccessType.READ_WRITE, LocalDateTime.now(clock).plusDays(1)
        ));

        userService.registerUser("bob", "Strong1!", "inv-token");
        Either<UserError, TokenExternalInfo> result = userService.registerUser("charlie", "Strong1!", "inv-token");

        EitherAssert.assertLeft(result, UserError.InvitationAlreadyUsed.class);
    }

    @Test
    void testUpdatePassword_UserNotFound() {
        Either<UserError, User> result = userService.updatePassword(999L, "Strong1!", "Stronger2@");

        EitherAssert.assertLeft(result, UserError.UserNotFound.class);
    }

    @Test
    void testRevokeToken_InvalidToken() {
        assertThatCode(() -> userService.revokeToken("invalid-token-format")).doesNotThrowAnyException();
    }

    @Test
    void testCleanupExpiredTokens() {
        assertThatCode(() -> userService.cleanupExpiredTokens()).doesNotThrowAnyException();
    }
}