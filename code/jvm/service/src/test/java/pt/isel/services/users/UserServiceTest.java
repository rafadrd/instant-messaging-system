package pt.isel.services.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.isel.domain.builders.TokenExternalInfoBuilder;
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
import pt.isel.services.AbstractServiceTest;
import pt.isel.services.builders.ParsedTokenBuilder;
import pt.isel.services.common.RateLimiter;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest extends AbstractServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private RateLimiter rateLimiter;

    private UserService userService;

    @BeforeEach
    void setUp() {
        super.setUpBaseState();

        lenient().when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "encoded_" + inv.getArgument(0));
        lenient().when(passwordEncoder.matches(anyString(), anyString())).thenAnswer(inv -> {
            String raw = inv.getArgument(0);
            String encoded = inv.getArgument(1);
            return encoded.equals("encoded_" + raw);
        });

        lenient().when(tokenService.createToken(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return new TokenExternalInfoBuilder().withTokenValue("token-" + id).withUserId(id).build();
        });

        lenient().when(tokenService.validateToken(anyString())).thenAnswer(inv -> {
            String token = inv.getArgument(0);
            if (token.startsWith("token-")) {
                Long id = Long.parseLong(token.substring(6));
                return new ParsedTokenBuilder().withJti("jti-" + id).withUserId(id).build();
            }
            return null;
        });

        lenient().when(rateLimiter.isRateLimited(anyString(), anyString(), anyInt(), any())).thenReturn(false);

        PasswordPolicyConfig config = new PasswordPolicyConfig(8, true, true, true, true);
        PasswordSecurityDomain securityDomain = new PasswordSecurityDomain(passwordEncoder, config);

        userService = new UserService(trxManager, securityDomain, tokenService, clock, rateLimiter);
    }

    @Test
    void testRegisterUser_Success() {
        Either<UserError, TokenExternalInfo> result = userService.registerUser("dave", "Strong1!", null);

        TokenExternalInfo tokenInfo = EitherAssert.assertRight(result);
        assertThat(tokenInfo.tokenValue()).isNotNull();
        assertThat(tokenInfo.userId()).isNotNull();
    }

    @Test
    void testRegisterUser_WithInvitation() {
        User owner = EitherAssert.assertRight(userService.getUserById(
                EitherAssert.assertRight(userService.registerUser("owner", "Strong1!", null)).userId()
        ));

        Channel channel = trxManager.run(trx -> insertChannel(trx, "Secret", owner, false));
        Invitation inv = trxManager.run(trx -> insertInvitation(trx, "inv-token", owner, channel, AccessType.READ_WRITE, LocalDateTime.now(clock).plusDays(1)));

        Either<UserError, TokenExternalInfo> result = userService.registerUser("eve", "Strong1!", inv.token());

        Long eveId = EitherAssert.assertRight(result).userId();

        trxManager.run(trx -> {
            assertThat(trx.repoMemberships().findUserInChannel(eveId, channel.id())).isNotNull();
            return null;
        });
    }

    @Test
    void testRegisterUser_InvitationExpired() {
        User owner = EitherAssert.assertRight(userService.getUserById(
                EitherAssert.assertRight(userService.registerUser("owner", "Strong1!", null)).userId()
        ));

        Channel channel = trxManager.run(trx -> insertChannel(trx, "Secret", owner, false));
        Invitation inv = trxManager.run(trx -> insertInvitation(trx, "expired-inv-token", owner, channel, AccessType.READ_WRITE, LocalDateTime.now(clock).minusDays(1)));

        Either<UserError, TokenExternalInfo> result = userService.registerUser("eve", "Strong1!", inv.token());

        EitherAssert.assertLeft(result, UserError.InvitationExpired.class);
    }

    @Test
    void testRegisterUser_InsecurePassword() {
        Either<UserError, TokenExternalInfo> result = userService.registerUser("dave", "weak", null);

        EitherAssert.assertLeft(result, UserError.InsecurePassword.class);
    }

    @Test
    void testRegisterUser_UsernameAlreadyInUse() {
        userService.registerUser("dave", "Strong1!", null);
        Either<UserError, TokenExternalInfo> result = userService.registerUser("dave", "Strong2!", null);

        EitherAssert.assertLeft(result, UserError.UsernameAlreadyInUse.class);
    }

    @Test
    void testGetUserById_Success() {
        Long id = EitherAssert.assertRight(userService.registerUser("dave", "Strong1!", null)).userId();
        Either<UserError, User> result = userService.getUserById(id);

        assertThat(EitherAssert.assertRight(result).username()).isEqualTo("dave");
    }

    @Test
    void testUpdateUsername_Success() {
        Long id = EitherAssert.assertRight(userService.registerUser("dave", "Strong1!", null)).userId();
        Either<UserError, User> result = userService.updateUsername(id, "dave_new", "Strong1!");

        assertThat(EitherAssert.assertRight(result).username()).isEqualTo("dave_new");
    }

    @Test
    void testUpdateUsername_UsernameAlreadyInUse() {
        Long id1 = EitherAssert.assertRight(userService.registerUser("dave", "Strong1!", null)).userId();
        userService.registerUser("eve", "Strong1!", null);

        Either<UserError, User> result = userService.updateUsername(id1, "eve", "Strong1!");

        EitherAssert.assertLeft(result, UserError.UsernameAlreadyInUse.class);
    }

    @Test
    void testUpdateUsername_IncorrectPassword() {
        Long id = EitherAssert.assertRight(userService.registerUser("dave", "Strong1!", null)).userId();
        Either<UserError, User> result = userService.updateUsername(id, "dave_new", "Wrong1!");

        EitherAssert.assertLeft(result, UserError.IncorrectPassword.class);
    }

    @Test
    void testUpdatePassword_Success() {
        Long id = EitherAssert.assertRight(userService.registerUser("dave", "Strong1!", null)).userId();
        Either<UserError, User> result = userService.updatePassword(id, "Strong1!", "Stronger2@");

        EitherAssert.assertRight(result);
    }

    @Test
    void testUpdatePassword_SameAsPrevious() {
        Long id = EitherAssert.assertRight(userService.registerUser("dave", "Strong1!", null)).userId();
        Either<UserError, User> result = userService.updatePassword(id, "Strong1!", "Strong1!");

        EitherAssert.assertLeft(result, UserError.PasswordSameAsPrevious.class);
    }

    @Test
    void testDeleteUser_Success() {
        Long id = EitherAssert.assertRight(userService.registerUser("dave", "Strong1!", null)).userId();
        Either<UserError, String> result = userService.deleteUser(id);

        EitherAssert.assertRight(result);
        EitherAssert.assertLeft(userService.getUserById(id));
    }

    @Test
    void testDeleteUser_HasOwnedChannels() {
        Long id = EitherAssert.assertRight(userService.registerUser("dave", "Strong1!", null)).userId();
        trxManager.run(trx -> insertChannel(trx, "General", trx.repoUsers().findById(id), true));

        Either<UserError, String> result = userService.deleteUser(id);

        EitherAssert.assertLeft(result, UserError.UserHasOwnedChannels.class);
    }

    @Test
    void testCreateToken_Success() {
        userService.registerUser("dave", "Strong1!", null);
        Either<UserError, TokenExternalInfo> result = userService.createToken("dave", "Strong1!");

        assertThat(EitherAssert.assertRight(result).tokenValue()).isNotNull();
    }

    @Test
    void testCreateToken_RateLimited() {
        userService.registerUser("dave", "Strong1!", null);
        when(rateLimiter.isRateLimited(anyString(), anyString(), anyInt(), any())).thenReturn(true);

        Either<UserError, TokenExternalInfo> result = userService.createToken("dave", "Strong1!");

        EitherAssert.assertLeft(result, UserError.RateLimitExceeded.class);
    }

    @Test
    void testGetUserByToken_Success() {
        Long id = EitherAssert.assertRight(userService.registerUser("dave", "Strong1!", null)).userId();
        User user = userService.getUserByToken("token-" + id);

        assertThat(user).isNotNull();
        assertThat(user.id()).isEqualTo(id);
    }

    @Test
    void testRevokeToken_Success() {
        Long id = EitherAssert.assertRight(userService.registerUser("dave", "Strong1!", null)).userId();
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
        EitherAssert.assertLeft(userService.registerUser("eve", invalidPassword, null));
    }

    @Test
    void testRegisterUser_InvitationNotFound() {
        Either<UserError, TokenExternalInfo> result = userService.registerUser("eve", "Strong1!", "invalid-token");

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
        Long id = EitherAssert.assertRight(userService.registerUser("dave", "Strong1!", null)).userId();
        EitherAssert.assertLeft(userService.updateUsername(id, invalidUsername, "Strong1!"));
    }

    @Test
    void testUpdateUsername_UsernameTooLong() {
        Long id = EitherAssert.assertRight(userService.registerUser("dave", "Strong1!", null)).userId();
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
        Long id = EitherAssert.assertRight(userService.registerUser("dave", "Strong1!", null)).userId();
        EitherAssert.assertLeft(userService.updatePassword(id, "Strong1!", invalidPassword));
    }

    @Test
    void testUpdatePassword_InsecurePassword() {
        Long id = EitherAssert.assertRight(userService.registerUser("dave", "Strong1!", null)).userId();
        Either<UserError, User> result = userService.updatePassword(id, "Strong1!", "weak");

        EitherAssert.assertLeft(result, UserError.InsecurePassword.class);
    }

    @Test
    void testUpdatePassword_IncorrectOldPassword() {
        Long id = EitherAssert.assertRight(userService.registerUser("dave", "Strong1!", null)).userId();
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
        EitherAssert.assertLeft(userService.createToken("dave", invalidInput));
    }

    @Test
    void testCreateToken_UserNotFound() {
        Either<UserError, TokenExternalInfo> result = userService.createToken("ghost", "Strong1!");

        EitherAssert.assertLeft(result, UserError.UserNotFound.class);
    }

    @Test
    void testCreateToken_IncorrectPassword() {
        userService.registerUser("dave", "Strong1!", null);
        Either<UserError, TokenExternalInfo> result = userService.createToken("dave", "Wrong1!");

        EitherAssert.assertLeft(result, UserError.IncorrectPassword.class);
    }

    @Test
    void testGetUserByToken_InvalidToken() {
        assertThat(userService.getUserByToken("invalid-format")).isNull();
    }

    @Test
    void testGetUserByToken_BlacklistedToken() {
        Long id = EitherAssert.assertRight(userService.registerUser("dave", "Strong1!", null)).userId();
        String token = "token-" + id;

        userService.revokeToken(token);
        assertThat(userService.getUserByToken(token)).isNull();
    }

    @Test
    void testRegisterUser_InvitationAlreadyUsed() {
        User owner = EitherAssert.assertRight(userService.getUserById(
                EitherAssert.assertRight(userService.registerUser("owner", "Strong1!", null)).userId()
        ));

        Channel channel = trxManager.run(trx -> insertChannel(trx, "Secret", owner, false));
        trxManager.run(trx -> insertInvitation(trx, "inv-token", owner, channel, AccessType.READ_WRITE, LocalDateTime.now(clock).plusDays(1)));

        userService.registerUser("eve", "Strong1!", "inv-token");
        Either<UserError, TokenExternalInfo> result = userService.registerUser("frank", "Strong1!", "inv-token");

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