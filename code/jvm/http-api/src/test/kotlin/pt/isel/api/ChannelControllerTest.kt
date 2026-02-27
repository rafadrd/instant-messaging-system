package pt.isel.api

import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.http.HttpStatus
import pt.isel.api.model.ChannelInput
import pt.isel.domain.channel.AccessType
import pt.isel.domain.channel.Channel
import pt.isel.domain.security.AuthenticatedUser
import pt.isel.domain.security.PasswordValidationInfo
import pt.isel.domain.user.User
import pt.isel.mem.TransactionManagerInMem
import pt.isel.repositories.TransactionManager
import pt.isel.repositories.jdbi.TransactionManagerJdbi
import pt.isel.repositories.jdbi.configureWithAppRequirements
import pt.isel.services.ChannelService
import pt.isel.services.MessageEventService
import java.util.stream.Stream
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class ChannelControllerTest {
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
    private lateinit var channelService: ChannelService
    private lateinit var channelController: ChannelController
    private lateinit var messageEventService: MessageEventService

    @BeforeTest
    fun setup() {
        trxManager = TransactionManagerInMem()
        channelService = ChannelService(trxManager)
        messageEventService = MessageEventService(trxManager)
        channelController = ChannelController(channelService, messageEventService)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getChannels - Success`() {
        trxManager.run {
            val user = repoUsers.create("user", PasswordValidationInfo("hash"))
            val authUser = AuthenticatedUser(user, "token")
            val channel = repoChannels.create("channel", user, true)
            val response = channelController.getChannels(authUser, channel.name)

            assertEquals(HttpStatus.OK, response.statusCode)
            assert(response.body is List<*>)
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createChannel - Success`() {
        trxManager.run {
            val user = repoUsers.create("user", PasswordValidationInfo("hash"))
            val input = ChannelInput("channel", true)
            val response = channelController.createChannel(AuthenticatedUser(user, "token"), input)

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals("channel", (response.body as Channel).name)
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createChannel - Failure, Channel name blank`() {
        trxManager.run {
            val user = repoUsers.create("user", PasswordValidationInfo("hash"))
            val input = ChannelInput("", true)
            val response = channelController.createChannel(AuthenticatedUser(user, "token"), input)

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            assertEquals(Problem.EmptyChannelName.title, (response.body as ProblemResponse).title)
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createChannel - Failure, User not found`() {
        val user = AuthenticatedUser(User(1, "user", PasswordValidationInfo("password")), "token")
        val input = ChannelInput("channel", true)
        val response = channelController.createChannel(user, input)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(Problem.UserNotFound.title, (response.body as ProblemResponse).title)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createChannel - Failure, Channel already exists`() {
        trxManager.run {
            val user = repoUsers.create("user", PasswordValidationInfo("hash"))
            val channel = repoChannels.create("channel", user, true)
            val input = ChannelInput(channel.name, true)
            val response = channelController.createChannel(AuthenticatedUser(user, "token"), input)

            assertEquals(HttpStatus.CONFLICT, response.statusCode)
            assertEquals(
                Problem.ChannelAlreadyExists.title,
                (response.body as ProblemResponse).title,
            )
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `joinChannel - Success`() {
        trxManager.run {
            val user = repoUsers.create("user", PasswordValidationInfo("hash"))
            val channel = repoChannels.create("channel", user, true)
            val authUser = AuthenticatedUser(user, "token")
            val response = channelController.joinChannel(authUser, channel.id)

            assertEquals(HttpStatus.OK, response.statusCode)
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `joinChannel - Failure, User not found`() {
        val user = User(1, "user", PasswordValidationInfo("hash"))
        val authUser = AuthenticatedUser(user, "token")
        val response = channelController.joinChannel(authUser, 1)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(Problem.UserNotFound.title, (response.body as ProblemResponse).title)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `joinChannel - Failure, Channel not found`() {
        trxManager.run {
            val user = repoUsers.create("user", PasswordValidationInfo("hash"))
            val authUser = AuthenticatedUser(user, "token")
            val response = channelController.joinChannel(authUser, 1)

            assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
            assertEquals(Problem.ChannelNotFound.title, (response.body as ProblemResponse).title)
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `joinChannel - Failure, User already in channel`() {
        trxManager.run {
            val user = repoUsers.create("user", PasswordValidationInfo("hash"))
            val channel = repoChannels.create("channel", user, true)
            repoMemberships.addUserToChannel(user, channel, AccessType.READ_WRITE)
            val authUser = AuthenticatedUser(user, "token")
            val response = channelController.joinChannel(authUser, channel.id)

            assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
            assertEquals(
                Problem.UserAlreadyInChannel.title,
                (response.body as ProblemResponse).title,
            )
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `leaveChannel - Success`() {
        trxManager.run {
            val user1 = repoUsers.create("user1", PasswordValidationInfo("hash"))
            val user2 = repoUsers.create("user2", PasswordValidationInfo("hash"))
            val channel = repoChannels.create("channel", user1, true)
            repoMemberships.addUserToChannel(user2, channel, AccessType.READ_WRITE)
            // val authUser = AuthenticatedUser(user1, "token")
            // val response = channelController.leaveChannel(authUser, channel.id, user2.id)
            val authUser = AuthenticatedUser(user2, "token")
            val response = channelController.leaveChannel(authUser, channel.id)

            assertEquals(HttpStatus.OK, response.statusCode)
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `leaveChannel - Failure, User not found`() {
        trxManager.run {
            val user1 = repoUsers.create("user1", PasswordValidationInfo("hash"))
            val user2 = User(2, "user2", PasswordValidationInfo("hash"))
            val channel = repoChannels.create("channel", user1, true)
            // val authUser = AuthenticatedUser(user1, "token")
            // val response = channelController.leaveChannel(authUser, channel.id, user2.id)
            val authUser = AuthenticatedUser(user2, "token")
            val response = channelController.leaveChannel(authUser, channel.id)

            assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
            assertEquals(Problem.UserNotFound.title, (response.body as ProblemResponse).title)
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `leaveChannel - Failure, Channel not found`() {
        trxManager.run {
            val user = repoUsers.create("user", PasswordValidationInfo("hash"))
            val channel = Channel(1, "channel", user, true)
            val authUser = AuthenticatedUser(user, "token")
            // val response = channelController.leaveChannel(authUser, channel.id, user.id)
            val response = channelController.leaveChannel(authUser, channel.id)

            assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
            assertEquals(Problem.ChannelNotFound.title, (response.body as ProblemResponse).title)
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `leaveChannel - Failure, User not in channel`() {
        trxManager.run {
            val user1 = repoUsers.create("user1", PasswordValidationInfo("hash"))
            val user2 = repoUsers.create("user2", PasswordValidationInfo("hash"))
            val channel = repoChannels.create("channel", user1, true)
            // val authUser = AuthenticatedUser(user1, "token")
            // val response = channelController.leaveChannel(authUser, channel.id, user2.id)
            val authUser = AuthenticatedUser(user2, "token")
            val response = channelController.leaveChannel(authUser, channel.id)

            assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
            assertEquals(Problem.UserNotInChannel.title, (response.body as ProblemResponse).title)
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `leaveChannel - Failure, Owner cannot leave`() {
        trxManager.run {
            val user = repoUsers.create("user", PasswordValidationInfo("hash"))
            val channel = repoChannels.create("channel", user, true)
            repoMemberships.addUserToChannel(user, channel, AccessType.READ_WRITE)
            val authUser = AuthenticatedUser(user, "token")
            // val response = channelController.leaveChannel(authUser, channel.id, user.id)
            val response = channelController.leaveChannel(authUser, channel.id)

            assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
            assertEquals(Problem.OwnerCannotLeave.title, (response.body as ProblemResponse).title)
        }
    }
}
