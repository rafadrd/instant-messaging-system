package pt.isel.services

import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pt.isel.auth.Sha256TokenEncoder
import pt.isel.auth.UsersDomainConfig
import pt.isel.domain.channel.AccessType
import pt.isel.domain.security.PasswordSecurityDomain
import pt.isel.mem.TransactionManagerInMem
import pt.isel.repositories.jdbi.TransactionManagerJdbi
import pt.isel.repositories.jdbi.configureWithAppRequirements
import java.time.LocalDateTime
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

const val VALID_PASSWORD = "Aa1#2345"
const val NEW_VALID_PASSWORD = "Aa1#2346"

class UserServiceTest {
    companion object {
        private val jdbi =
            Jdbi
                .create(PGSimpleDataSource().apply { setURL(Environment.getDbUrl()) })
                .configureWithAppRequirements()

        @JvmStatic
        fun transactionManagers(): Stream<TransactionManager> =
            Stream.of(
                TransactionManagerInMem().also { cleanup(it) },
                TransactionManagerJdbi(jdbi).also { cleanup(it) },
            )

        private fun cleanup(trxManager: TransactionManager) {
            trxManager.run {
                repoInvitations.clear()
                repoMemberships.clear()
                repoMessages.clear()
                repoChannels.clear()
                repoUsers.clear()
            }
        }

        private fun createUserService(
            trxManager: TransactionManager,
            testClock: TestClock,
            tokenTtl: Duration = 30.days,
            tokenRollingTtl: Duration = 30.minutes,
            maxTokensPerUser: Int = 3,
        ) = UserService(
            trxManager,
            PasswordSecurityDomain(
                BCryptPasswordEncoder(),
                Sha256TokenEncoder(),
                UsersDomainConfig(
                    tokenSizeInBytes = 256 / 8,
                    tokenTtl = tokenTtl,
                    tokenRollingTtl,
                    maxTokensPerUser = maxTokensPerUser,
                ),
            ),
            testClock,
        )
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `create a valid user`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user = userService.registerUser("username", VALID_PASSWORD)
        assertTrue(user is Either.Right)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `create a user with empty username`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user = userService.registerUser("", "password")
        assertTrue(user is Either.Left)
        assertEquals(UserError.EmptyUsername, user.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `create a user with empty password`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user = userService.registerUser("username", "")
        assertTrue(user is Either.Left)
        assertEquals(UserError.EmptyPassword, user.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `create a user with insecure password`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user = userService.registerUser("username", "notValid")
        assertTrue(user is Either.Left)
        assertEquals(UserError.InsecurePassword, user.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `create a user with an already existing username`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user = userService.registerUser("username", VALID_PASSWORD)
        assertTrue(user is Either.Right)
        val channelService = ChannelService(trxManager)
        val channel = channelService.createChannel("channelName", user.value.id, true)
        assertTrue(channel is Either.Right)
        val invitationService = InvitationService(trxManager)
        val invitation =
            invitationService.createInvitation(
                user.value.id,
                channel.value.id,
                AccessType.READ_ONLY,
                LocalDateTime.now().plusDays(7),
            )
        assertTrue(invitation is Either.Right)
        val user2 = userService.registerUser("username", VALID_PASSWORD, invitation.value.token)
        assertTrue(user2 is Either.Left)
        assertEquals(UserError.UsernameAlreadyInUse, user2.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `get user by username`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        userService.registerUser("username", VALID_PASSWORD)
        val user = userService.getUserByUsername("username")
        assertTrue(user is Either.Right)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `get user by username with empty username`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user = userService.getUserByUsername("")
        assertTrue(user is Either.Left)
        assertEquals(UserError.EmptyUsername, user.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `get user by username that does not exist`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user = userService.getUserByUsername("username")
        assertTrue(user is Either.Left)
        assertEquals(UserError.UserNotFound, user.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `get user by id`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user = userService.registerUser("username", VALID_PASSWORD)
        assertTrue(user is Either.Right)
        val userAut = user.value
        val userIdResult = userService.getUserById(userAut.id)
        assertTrue(userIdResult is Either.Right)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `get user by id that does not exist`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user = userService.getUserById(1)
        assertTrue(user is Either.Left)
        assertEquals(UserError.UserNotFound, user.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `update valid username`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user = userService.registerUser("username", VALID_PASSWORD)
        val userAut = (user as Either.Right).value
        val updatedResult = userService.updateUsername(userAut.id, "newUsername", VALID_PASSWORD)
        assertTrue(updatedResult is Either.Right)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `update username with empty username`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user = userService.registerUser("username", VALID_PASSWORD)
        val userAut = (user as Either.Right).value
        val updatedResult = userService.updateUsername(userAut.id, "", VALID_PASSWORD)
        assertTrue(updatedResult is Either.Left)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `update username with already existing username`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user1 = userService.registerUser("username", VALID_PASSWORD)
        val user1Aut = (user1 as Either.Right).value
        val channelService = ChannelService(trxManager)
        val channel = channelService.createChannel("channelName", user1.value.id, true)
        assertTrue(channel is Either.Right)
        val invitationService = InvitationService(trxManager)
        val invitation =
            invitationService.createInvitation(
                user1Aut.id,
                channel.value.id,
                AccessType.READ_ONLY,
                LocalDateTime.now().plusDays(7),
            )
        assertTrue(invitation is Either.Right)
        val user2 = userService.registerUser("username2", VALID_PASSWORD, invitation.value.token)
        assertTrue(user2 is Either.Right)
        val updatedResult = userService.updateUsername(user1Aut.id, "username2", VALID_PASSWORD)
        assertTrue(updatedResult is Either.Left)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `update username of a non-existing user`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val updatedUsername = userService.updateUsername(1, "newUsername", "password")
        assertTrue(updatedUsername is Either.Left)
        assertEquals(UserError.UserNotFound, updatedUsername.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `update valid password`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user = userService.registerUser("username", VALID_PASSWORD)
        val userAut = (user as Either.Right).value
        val updatePassword = userService.updatePassword(userAut.id, NEW_VALID_PASSWORD)
        assertTrue(updatePassword is Either.Right)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `update with the same password`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user = userService.registerUser("username", VALID_PASSWORD)
        val userAut = (user as Either.Right).value
        val updatePassword = userService.updatePassword(userAut.id, VALID_PASSWORD)
        assertTrue(updatePassword is Either.Left)
        assertEquals(UserError.PasswordSameAsPrevious, updatePassword.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `update password with empty password`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user = userService.registerUser("username", VALID_PASSWORD)
        val userAut = (user as Either.Right).value
        val updatePassword = userService.updatePassword(userAut.id, "")
        assertTrue(updatePassword is Either.Left)
        assertEquals(UserError.EmptyPassword, updatePassword.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `update password of a non-existing user`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val updatePassword = userService.updatePassword(1, NEW_VALID_PASSWORD)
        assertTrue(updatePassword is Either.Left)
        assertEquals(UserError.UserNotFound, updatePassword.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `delete a valid user`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user = userService.registerUser("username", VALID_PASSWORD)
        val userAut = (user as Either.Right).value
        val delete = userService.deleteUser(userAut.id)
        assertTrue(delete is Either.Right)
        assertEquals("User ${userAut.id} deleted", delete.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `delete a non-existing user`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val delete = userService.deleteUser(1)
        assertTrue(delete is Either.Left)
        assertEquals(UserError.UserNotFound, delete.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `create token with valid credentials`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        userService.registerUser("username", VALID_PASSWORD)
        val tokenResult = userService.createToken("username", VALID_PASSWORD)
        assertTrue(tokenResult is Either.Right)
        assertNotNull(tokenResult.value.tokenValue)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `create token with invalid username`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val tokenResult = userService.createToken("invalidUsername", VALID_PASSWORD)
        assertTrue(tokenResult is Either.Left)
        assertEquals(UserError.UserNotFound, tokenResult.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `create token with invalid password`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        userService.registerUser("username", VALID_PASSWORD)
        val tokenResult = userService.createToken("username", "invalidPassword")
        assertTrue(tokenResult is Either.Left)
        assertEquals(UserError.IncorrectPassword, tokenResult.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `revoke valid token`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        userService.registerUser("username", VALID_PASSWORD)
        val tokenResult = userService.createToken("username", VALID_PASSWORD)
        assertTrue(tokenResult is Either.Right)
        val revokeResult = userService.revokeToken(tokenResult.value.tokenValue)
        assertTrue(revokeResult)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `revoke invalid token`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val revokeResult = userService.revokeToken("invalidToken")
        assertTrue(revokeResult)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `get user by valid token`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        userService.registerUser("username", VALID_PASSWORD)
        val tokenResult = userService.createToken("username", VALID_PASSWORD)
        assertTrue(tokenResult is Either.Right)
        val user = userService.getUserByToken(tokenResult.value.tokenValue)
        assertNotNull(user)
        assertEquals("username", user.username)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `get user by invalid token`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user = userService.getUserByToken("invalidToken")
        assertNull(user)
    }
}
