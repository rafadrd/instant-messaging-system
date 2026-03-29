package pt.isel.services.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.common.Either;
import pt.isel.domain.common.EitherAssert;
import pt.isel.domain.common.UserError;
import pt.isel.domain.fakes.FakePasswordEncoder;
import pt.isel.domain.invitations.Invitation;
import pt.isel.domain.security.PasswordPolicyConfig;
import pt.isel.domain.security.PasswordSecurityDomain;
import pt.isel.domain.security.TokenExternalInfo;
import pt.isel.domain.users.User;
import pt.isel.services.AbstractServiceTest;
import pt.isel.services.fakes.FakeRateLimiter;
import pt.isel.services.fakes.FakeTokenService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class UserServiceTest extends AbstractServiceTest {

    private FakeRateLimiter rateLimiter;
    private UserService userService;

    @BeforeEach
    void setUp() {
        super.setUpBaseState();
        FakePasswordEncoder passwordEncoder = new FakePasswordEncoder();
        FakeTokenService tokenService = new FakeTokenService();
        rateLimiter = new FakeRateLimiter();

        PasswordPolicyConfig config = new PasswordPolicyConfig(8, true, true, true, true);
        PasswordSecurityDomain securityDomain = new PasswordSecurityDomain(passwordEncoder, config);

        userService = new UserService(trxManager, securityDomain, tokenService, clock, rateLimiter);
    }


    @Test
    void RegisterUser_ValidInput_ReturnsSuccess() {
        Either<UserError, TokenExternalInfo> result = userService.registerUser("dave", "Strong1!", null);

        TokenExternalInfo tokenInfo = EitherAssert.assertThat(result).isRight().getRightValue();
        assertThat(tokenInfo.tokenValue()).isNotNull();
        assertThat(tokenInfo.userId()).isNotNull();
    }

    @Test
    void RegisterUser_WithInvitation_ReturnsSuccess() {
        User owner = EitherAssert.assertThat(userService.getUserById(
                EitherAssert.assertThat(userService.registerUser("owner", "Strong1!", null)).isRight().getRightValue().userId()
        )).isRight().getRightValue();
        Channel channel = trxManager.run(trx -> insertChannel(trx, "Secret", owner, false));
        Invitation inv = trxManager.run(trx -> insertInvitation(trx, "inv-token", owner, channel, AccessType.READ_WRITE, LocalDateTime.now(clock).plusDays(1)));

        Either<UserError, TokenExternalInfo> result = userService.registerUser("eve", "Strong1!", inv.token());

        Long eveId = EitherAssert.assertThat(result).isRight().getRightValue().userId();
        trxManager.run(trx -> {
            assertThat(trx.repoMemberships().findUserInChannel(eveId, channel.id())).isNotNull();
            return null;
        });
    }

    @Test
    void RegisterUser_InvitationExpired_ReturnsLeft() {
        User owner = EitherAssert.assertThat(userService.getUserById(
                EitherAssert.assertThat(userService.registerUser("owner", "Strong1!", null)).isRight().getRightValue().userId()
        )).isRight().getRightValue();
        Channel channel = trxManager.run(trx -> insertChannel(trx, "Secret", owner, false));
        Invitation inv = trxManager.run(trx -> insertInvitation(trx, "expired-inv-token", owner, channel, AccessType.READ_WRITE, LocalDateTime.now(clock).minusDays(1)));

        Either<UserError, TokenExternalInfo> result = userService.registerUser("eve", "Strong1!", inv.token());

        EitherAssert.assertThat(result).isLeftInstanceOf(UserError.InvitationExpired.class);
    }

    @Test
    void RegisterUser_InsecurePassword_ReturnsLeft() {
        Either<UserError, TokenExternalInfo> result = userService.registerUser("dave", "weak", null);

        EitherAssert.assertThat(result).isLeftInstanceOf(UserError.InsecurePassword.class);
    }

    @Test
    void RegisterUser_UsernameAlreadyInUse_ReturnsLeft() {
        userService.registerUser("dave", "Strong1!", null);

        Either<UserError, TokenExternalInfo> result = userService.registerUser("dave", "Strong2!", null);

        EitherAssert.assertThat(result).isLeftInstanceOf(UserError.UsernameAlreadyInUse.class);
    }

    @Test
    void GetUserById_ValidId_ReturnsSuccess() {
        Long id = EitherAssert.assertThat(userService.registerUser("dave", "Strong1!", null)).isRight().getRightValue().userId();

        Either<UserError, User> result = userService.getUserById(id);

        User user = EitherAssert.assertThat(result).isRight().getRightValue();
        assertThat(user.username()).isEqualTo("dave");
    }

    @Test
    void UpdateUsername_ValidInput_ReturnsSuccess() {
        Long id = EitherAssert.assertThat(userService.registerUser("dave", "Strong1!", null)).isRight().getRightValue().userId();

        Either<UserError, User> result = userService.updateUsername(id, "dave_new", "Strong1!");

        User updated = EitherAssert.assertThat(result).isRight().getRightValue();
        assertThat(updated.username()).isEqualTo("dave_new");
    }

    @Test
    void UpdateUsername_UsernameAlreadyInUse_ReturnsLeft() {
        Long id1 = EitherAssert.assertThat(userService.registerUser("dave", "Strong1!", null)).isRight().getRightValue().userId();
        userService.registerUser("eve", "Strong1!", null);

        Either<UserError, User> result = userService.updateUsername(id1, "eve", "Strong1!");

        EitherAssert.assertThat(result).isLeftInstanceOf(UserError.UsernameAlreadyInUse.class);
    }

    @Test
    void UpdateUsername_IncorrectPassword_ReturnsLeft() {
        Long id = EitherAssert.assertThat(userService.registerUser("dave", "Strong1!", null)).isRight().getRightValue().userId();

        Either<UserError, User> result = userService.updateUsername(id, "dave_new", "Wrong1!");

        EitherAssert.assertThat(result).isLeftInstanceOf(UserError.IncorrectPassword.class);
    }

    @Test
    void UpdatePassword_ValidInput_ReturnsSuccess() {
        Long id = EitherAssert.assertThat(userService.registerUser("dave", "Strong1!", null)).isRight().getRightValue().userId();

        Either<UserError, User> result = userService.updatePassword(id, "Strong1!", "Stronger2@");

        User updated = EitherAssert.assertThat(result).isRight().getRightValue();
        assertThat(updated.id()).isEqualTo(id);
    }

    @Test
    void UpdatePassword_SameAsPrevious_ReturnsLeft() {
        Long id = EitherAssert.assertThat(userService.registerUser("dave", "Strong1!", null)).isRight().getRightValue().userId();

        Either<UserError, User> result = userService.updatePassword(id, "Strong1!", "Strong1!");

        EitherAssert.assertThat(result).isLeftInstanceOf(UserError.PasswordSameAsPrevious.class);
    }

    @Test
    void DeleteUser_ValidInput_ReturnsSuccess() {
        Long id = EitherAssert.assertThat(userService.registerUser("dave", "Strong1!", null)).isRight().getRightValue().userId();

        Either<UserError, String> result = userService.deleteUser(id);

        EitherAssert.assertThat(result).containsRight("User " + id + " deleted");
        EitherAssert.assertThat(userService.getUserById(id)).isLeftInstanceOf(UserError.UserNotFound.class);
    }

    @Test
    void DeleteUser_HasOwnedChannels_ReturnsLeft() {
        Long id = EitherAssert.assertThat(userService.registerUser("dave", "Strong1!", null)).isRight().getRightValue().userId();
        trxManager.run(trx -> insertChannel(trx, "General", trx.repoUsers().findById(id), true));

        Either<UserError, String> result = userService.deleteUser(id);

        EitherAssert.assertThat(result).isLeftInstanceOf(UserError.UserHasOwnedChannels.class);
    }

    @Test
    void CreateToken_ValidInput_ReturnsSuccess() {
        userService.registerUser("dave", "Strong1!", null);

        Either<UserError, TokenExternalInfo> result = userService.createToken("dave", "Strong1!");

        TokenExternalInfo tokenInfo = EitherAssert.assertThat(result).isRight().getRightValue();
        assertThat(tokenInfo.tokenValue()).isNotNull();
    }

    @Test
    void CreateToken_RateLimited_ReturnsLeft() {
        userService.registerUser("dave", "Strong1!", null);
        rateLimiter.setRateLimited(true);

        Either<UserError, TokenExternalInfo> result = userService.createToken("dave", "Strong1!");

        EitherAssert.assertThat(result).isLeftInstanceOf(UserError.RateLimitExceeded.class);
    }

    @Test
    void GetUserByToken_ValidToken_ReturnsUser() {
        Long id = EitherAssert.assertThat(userService.registerUser("dave", "Strong1!", null)).isRight().getRightValue().userId();

        User user = userService.getUserByToken("token-" + id);

        assertThat(user).isNotNull();
        assertThat(user.id()).isEqualTo(id);
    }

    @Test
    void RevokeToken_ValidToken_RevokesToken() {
        Long id = EitherAssert.assertThat(userService.registerUser("dave", "Strong1!", null)).isRight().getRightValue().userId();
        String token = "token-" + id;
        assertThat(userService.getUserByToken(token)).isNotNull();

        userService.revokeToken(token);

        assertThat(userService.getUserByToken(token)).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void RegisterUser_InvalidUsername_ReturnsLeft(String invalidUsername) {
        Either<UserError, TokenExternalInfo> result = userService.registerUser(invalidUsername, "Strong1!", null);

        EitherAssert.assertThat(result).isLeftInstanceOf(UserError.EmptyUsername.class);
    }

    @Test
    void RegisterUser_UsernameTooLong_ReturnsLeft() {
        String longName = "a".repeat(31);

        Either<UserError, TokenExternalInfo> result = userService.registerUser(longName, "Strong1!", null);

        EitherAssert.assertThat(result).isLeftInstanceOf(UserError.InvalidUsernameLength.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void RegisterUser_InvalidPassword_ReturnsLeft(String invalidPassword) {
        Either<UserError, TokenExternalInfo> result = userService.registerUser("eve", invalidPassword, null);

        EitherAssert.assertThat(result).isLeftInstanceOf(UserError.EmptyPassword.class);
    }

    @Test
    void RegisterUser_InvitationNotFound_ReturnsLeft() {
        Either<UserError, TokenExternalInfo> result = userService.registerUser("eve", "Strong1!", "invalid-token");

        EitherAssert.assertThat(result).isLeftInstanceOf(UserError.InvitationNotFound.class);
    }

    @Test
    void GetUserById_InvalidId_ReturnsLeft() {
        Either<UserError, User> result = userService.getUserById(999L);

        EitherAssert.assertThat(result).isLeftInstanceOf(UserError.UserNotFound.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void UpdateUsername_InvalidUsername_ReturnsLeft(String invalidUsername) {
        Long id = EitherAssert.assertThat(userService.registerUser("dave", "Strong1!", null)).isRight().getRightValue().userId();

        Either<UserError, User> result = userService.updateUsername(id, invalidUsername, "Strong1!");

        EitherAssert.assertThat(result).isLeftInstanceOf(UserError.EmptyUsername.class);
    }

    @Test
    void UpdateUsername_UsernameTooLong_ReturnsLeft() {
        Long id = EitherAssert.assertThat(userService.registerUser("dave", "Strong1!", null)).isRight().getRightValue().userId();
        String longName = "a".repeat(31);

        Either<UserError, User> result = userService.updateUsername(id, longName, "Strong1!");

        EitherAssert.assertThat(result).isLeftInstanceOf(UserError.InvalidUsernameLength.class);
    }

    @Test
    void UpdateUsername_UserNotFound_ReturnsLeft() {
        Either<UserError, User> result = userService.updateUsername(999L, "new_name", "Strong1!");

        EitherAssert.assertThat(result).isLeftInstanceOf(UserError.UserNotFound.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void UpdatePassword_InvalidPassword_ReturnsLeft(String invalidPassword) {
        Long id = EitherAssert.assertThat(userService.registerUser("dave", "Strong1!", null)).isRight().getRightValue().userId();

        Either<UserError, User> result = userService.updatePassword(id, "Strong1!", invalidPassword);

        EitherAssert.assertThat(result).isLeftInstanceOf(UserError.EmptyPassword.class);
    }

    @Test
    void UpdatePassword_InsecurePassword_ReturnsLeft() {
        Long id = EitherAssert.assertThat(userService.registerUser("dave", "Strong1!", null)).isRight().getRightValue().userId();

        Either<UserError, User> result = userService.updatePassword(id, "Strong1!", "weak");

        EitherAssert.assertThat(result).isLeftInstanceOf(UserError.InsecurePassword.class);
    }

    @Test
    void UpdatePassword_IncorrectOldPassword_ReturnsLeft() {
        Long id = EitherAssert.assertThat(userService.registerUser("dave", "Strong1!", null)).isRight().getRightValue().userId();

        Either<UserError, User> result = userService.updatePassword(id, "Wrong1!", "Stronger2@");

        EitherAssert.assertThat(result).isLeftInstanceOf(UserError.IncorrectPassword.class);
    }

    @Test
    void DeleteUser_InvalidId_ReturnsLeft() {
        Either<UserError, String> result = userService.deleteUser(999L);

        EitherAssert.assertThat(result).isLeftInstanceOf(UserError.UserNotFound.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void CreateToken_InvalidCredentials_ReturnsLeft(String invalidInput) {
        Either<UserError, TokenExternalInfo> result1 = userService.createToken(invalidInput, "Strong1!");
        Either<UserError, TokenExternalInfo> result2 = userService.createToken("dave", invalidInput);

        EitherAssert.assertThat(result1).isLeftInstanceOf(UserError.EmptyUsername.class);
        EitherAssert.assertThat(result2).isLeftInstanceOf(UserError.EmptyPassword.class);
    }

    @Test
    void CreateToken_UserNotFound_ReturnsLeft() {
        Either<UserError, TokenExternalInfo> result = userService.createToken("ghost", "Strong1!");

        EitherAssert.assertThat(result).isLeftInstanceOf(UserError.UserNotFound.class);
    }

    @Test
    void CreateToken_IncorrectPassword_ReturnsLeft() {
        userService.registerUser("dave", "Strong1!", null);

        Either<UserError, TokenExternalInfo> result = userService.createToken("dave", "Wrong1!");

        EitherAssert.assertThat(result).isLeftInstanceOf(UserError.IncorrectPassword.class);
    }

    @Test
    void GetUserByToken_InvalidToken_ReturnsNull() {
        User user = userService.getUserByToken("invalid-format");

        assertThat(user).isNull();
    }

    @Test
    void GetUserByToken_BlacklistedToken_ReturnsNull() {
        Long id = EitherAssert.assertThat(userService.registerUser("dave", "Strong1!", null)).isRight().getRightValue().userId();
        String token = "token-" + id;
        userService.revokeToken(token);

        User user = userService.getUserByToken(token);

        assertThat(user).isNull();
    }

    @Test
    void RegisterUser_InvitationAlreadyUsed_ReturnsLeft() {
        User owner = EitherAssert.assertThat(userService.getUserById(
                EitherAssert.assertThat(userService.registerUser("owner", "Strong1!", null)).isRight().getRightValue().userId()
        )).isRight().getRightValue();
        Channel channel = trxManager.run(trx -> insertChannel(trx, "Secret", owner, false));
        trxManager.run(trx -> insertInvitation(trx, "inv-token", owner, channel, AccessType.READ_WRITE, LocalDateTime.now(clock).plusDays(1)));
        userService.registerUser("eve", "Strong1!", "inv-token");

        Either<UserError, TokenExternalInfo> result = userService.registerUser("frank", "Strong1!", "inv-token");

        EitherAssert.assertThat(result).isLeftInstanceOf(UserError.InvitationAlreadyUsed.class);
    }

    @Test
    void UpdatePassword_UserNotFound_ReturnsLeft() {
        Either<UserError, User> result = userService.updatePassword(999L, "Strong1!", "Stronger2@");

        EitherAssert.assertThat(result).isLeftInstanceOf(UserError.UserNotFound.class);
    }

    @Test
    void RevokeToken_InvalidToken_DoesNotThrow() {
        assertThatCode(() -> userService.revokeToken("invalid-token-format")).doesNotThrowAnyException();
    }

    @Test
    void CleanupExpiredTokens_ValidState_DoesNotThrow() {
        assertThatCode(() -> userService.cleanupExpiredTokens()).doesNotThrowAnyException();
    }
}