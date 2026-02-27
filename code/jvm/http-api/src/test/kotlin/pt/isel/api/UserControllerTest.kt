package pt.isel.api

import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pt.isel.api.common.Problem
import pt.isel.api.common.ProblemResponse
import pt.isel.api.users.RegisterInput
import pt.isel.api.users.UserController
import pt.isel.api.users.UserInput
import pt.isel.auth.Sha256TokenEncoder
import pt.isel.auth.UsersDomainConfig
import pt.isel.domain.security.PasswordSecurityDomain
import pt.isel.domain.security.PasswordValidationInfo
import pt.isel.domain.users.AuthenticatedUser
import pt.isel.domain.users.User
import pt.isel.mem.TransactionManagerInMem
import pt.isel.repositories.TransactionManager
import pt.isel.repositories.jdbi.transaction.TransactionManagerJdbi
import pt.isel.repositories.jdbi.utils.configureWithAppRequirements
import pt.isel.services.channels.ChannelService
import pt.isel.services.users.UserService
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.hours

class UserControllerTest {
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
                repoUsers.clear()
                repoMemberships.clear()
                repoChannels.clear()
                repoMessages.clear()
                repoInvitations.clear()
            }
        }
    }

    private lateinit var trxManager: TransactionManager
    private lateinit var passwordSecurityDomain: PasswordSecurityDomain
    private lateinit var userService: UserService
    private lateinit var userController: UserController
    private lateinit var channelService: ChannelService

    @BeforeEach
    fun setup() {
        trxManager = TransactionManagerInMem()
        passwordSecurityDomain =
            PasswordSecurityDomain(
                passwordEncoder = BCryptPasswordEncoder(),
                tokenEncoder = Sha256TokenEncoder(),
                config =
                    UsersDomainConfig(
                        tokenSizeInBytes = 256 / 8,
                        tokenTtl = 24.hours,
                        tokenRollingTtl = 1.hours,
                        maxTokensPerUser = 3,
                    ),
            )
        userService = UserService(trxManager, passwordSecurityDomain, TestClock())
        channelService = ChannelService(trxManager)
        userController = UserController(userService, channelService)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createUser - Success`() {
        val user = RegisterInput("user", "SecureP@ssword1")
        val response = userController.registerUser(user)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertNotNull(response.headers["Location"])
        assert(response.headers["Location"]!!.first().contains("/api/users/"))
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createUser - Failure Empty username`() {
        val user = RegisterInput("", "SecureP@ssword1")
        val response = userController.registerUser(user)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(Problem.EmptyUsername.title, (response.body as ProblemResponse).title)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createUser - Failure Empty password`() {
        val user = RegisterInput("user", password = "")
        val response = userController.registerUser(user)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(Problem.EmptyPassword.title, (response.body as ProblemResponse).title)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createUser - Failure Insecure password`() {
        val user = RegisterInput("user", "password")
        val response = userController.registerUser(user)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(Problem.InsecurePassword.title, (response.body as ProblemResponse).title)
    }

    /*
    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createUser - Failure Username already exists`() {
        val user = UserInput("user", "SecureP@ssword1")
        userController.registerUser(user)
        val response = userController.registerUser(user)

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals(Problem.UsernameAlreadyInUse.title, (response.body as Problem).title)
    }

    /*
    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `token - Success`() {
        val user = UserInput("user", "SecureP@ssword1")
        userController.createUser(user)
        val token = UserInput("user", "SecureP@ssword1")
        val response = userController.token(token)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body as String)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `token - Failure Invalid username or password`() {
        val user = UserInput("user", "SecureP@ssword1")
        userController.createUser(user)
        val token = UserInput("user", "wrongPassword")
        val response = userController.token(token)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(Problem.UserOrPasswordAreInvalid.title, (response.body as Problem).title)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `logout - Success`() {
        val user = UserInput("user", "SecureP@ssword1")
        userController.createUser(user)
        val token = UserInput("user", "SecureP@ssword1")
        val response = userController.token(token)
        val authenticatedUser =
            AuthenticatedUser(
                User(1, "user", PasswordValidationInfo("hash")),
                response.body as String
            )

        userController.logout(authenticatedUser)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `userHome - Success`() {
        val user = UserInput("user", "SecureP@ssword1")
        userController.createUser(user)
        val token = UserInput("user", "SecureP@ssword1")
        val tokenResponse = userController.token(token)
        val authenticatedUser =
            AuthenticatedUser(
                User(1, "user", PasswordValidationInfo("hash")),
                tokenResponse.body as String,
            )
        val response = userController.userHome(authenticatedUser)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("user", response.body?.username)
    }
     */

     */

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `loginUser - Success`() {
        val user = RegisterInput("user", "SecureP@ssword1")
        userController.registerUser(user)
        val login = UserInput("user", "SecureP@ssword1")
        val response = userController.loginUser(login)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `loginUser - Failure Invalid login credentials`() {
        val user = RegisterInput("user", "SecureP@ssword1")
        userController.registerUser(user)
        val login = UserInput("user", "wrongPassword")
        val response = userController.loginUser(login)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals(Problem.IncorrectPassword.title, (response.body as ProblemResponse).title)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getUserChannels - Success`() {
        val userInput = RegisterInput("user", "SecureP@ssword1")
        userController.registerUser(userInput)
        val user = User(1, "user", PasswordValidationInfo("hash"))
        val authUser = AuthenticatedUser(user, "token")
        val response = userController.getUserChannels(authUser)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getUserChannels - Failure User not found`() {
        val user = User(1, "user", PasswordValidationInfo("hash"))
        val authUser = AuthenticatedUser(user, "token")
        val response = userController.getUserChannels(authUser)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(Problem.UserNotFound.title, (response.body as ProblemResponse).title)
    }
}
