package pt.isel.api

import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.http.HttpStatus
import pt.isel.api.common.Problem
import pt.isel.api.common.ProblemResponse
import pt.isel.api.invitations.InvitationController
import pt.isel.api.invitations.InvitationInput
import pt.isel.domain.channels.AccessType
import pt.isel.domain.channels.Channel
import pt.isel.domain.invitations.Invitation
import pt.isel.domain.security.PasswordValidationInfo
import pt.isel.domain.users.AuthenticatedUser
import pt.isel.domain.users.User
import pt.isel.mem.TransactionManagerInMem
import pt.isel.repositories.TransactionManager
import pt.isel.repositories.jdbi.transaction.TransactionManagerJdbi
import pt.isel.repositories.jdbi.utils.configureWithAppRequirements
import pt.isel.services.invitations.InvitationService
import java.time.LocalDateTime
import java.util.stream.Stream
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class InvitationControllerTest {
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
    private lateinit var invitationService: InvitationService
    private lateinit var invitationController: InvitationController

    @BeforeTest
    fun setup() {
        trxManager = TransactionManagerInMem()
        invitationService = InvitationService(trxManager)
        invitationController = InvitationController(invitationService)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createInvitation - Success`() {
        trxManager.run {
            val user = repoUsers.create("user", PasswordValidationInfo("hash"))
            val channel = repoChannels.create("channel", user, false)
            repoMemberships.addUserToChannel(user, channel, AccessType.READ_WRITE)
            val expiration = LocalDateTime.now().plusDays(1)
            val input = InvitationInput(AccessType.READ_ONLY, expiration)
            val authUser = AuthenticatedUser(user, "token")
            val response = invitationController.createInvitation(authUser, channel.id, input)

            assertEquals(HttpStatus.OK, response.statusCode)
            assert(response.body is Invitation)
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createInvitation - Failure User not found`() {
        trxManager.run {
            val user = User(1, "user", PasswordValidationInfo("hash"))
            val channel = repoChannels.create("channel", user, false)
            val expiration = LocalDateTime.now().plusDays(1)
            val input = InvitationInput(AccessType.READ_ONLY, expiration)
            val authUser = AuthenticatedUser(user, "token")
            val response = invitationController.createInvitation(authUser, channel.id, input)

            assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
            assertEquals(Problem.UserNotFound.title, (response.body as ProblemResponse).title)
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createInvitation - Failure Channel not found`() {
        trxManager.run {
            val user = repoUsers.create("user", PasswordValidationInfo("hash"))
            val channel = Channel(1, "channel", user, false)
            val expiration = LocalDateTime.now().plusDays(1)
            val input = InvitationInput(AccessType.READ_ONLY, expiration)
            val authUser = AuthenticatedUser(user, "token")
            val response = invitationController.createInvitation(authUser, channel.id, input)

            assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
            assertEquals(Problem.ChannelNotFound.title, (response.body as ProblemResponse).title)
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createInvitation - Failure User not authorized`() {
        trxManager.run {
            val user1 = repoUsers.create("user1", PasswordValidationInfo("hash"))
            val user2 = repoUsers.create("user2", PasswordValidationInfo("hash"))
            val channel = repoChannels.create("channel", user1, false)
            repoMemberships.addUserToChannel(user1, channel, AccessType.READ_WRITE)
            repoMemberships.addUserToChannel(user2, channel, AccessType.READ_ONLY)
            val expiration = LocalDateTime.now().plusDays(1)
            val input = InvitationInput(AccessType.READ_ONLY, expiration)
            val authUser = AuthenticatedUser(user2, "token")
            val response = invitationController.createInvitation(authUser, channel.id, input)

            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
            assertEquals(Problem.UserNotAuthorized.title, (response.body as ProblemResponse).title)
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createInvitation - Failure Invalid expiration time`() {
        trxManager.run {
            val user = repoUsers.create("user", PasswordValidationInfo("hash"))
            val channel = repoChannels.create("channel", user, false)
            repoMemberships.addUserToChannel(user, channel, AccessType.READ_WRITE)
            val expiration = LocalDateTime.now().minusDays(1)
            val input = InvitationInput(AccessType.READ_ONLY, expiration)
            val authUser = AuthenticatedUser(user, "token")
            val response = invitationController.createInvitation(authUser, channel.id, input)

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            assertEquals(
                Problem.InvalidExpirationTime.title,
                (response.body as ProblemResponse).title,
            )
        }
    }
}
