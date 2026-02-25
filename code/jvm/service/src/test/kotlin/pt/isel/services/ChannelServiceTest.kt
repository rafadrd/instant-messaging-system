package pt.isel.services

import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pt.isel.auth.Sha256TokenEncoder
import pt.isel.auth.UsersDomainConfig
import pt.isel.domain.AccessType
import pt.isel.domain.User
import pt.isel.domain.auth.PasswordSecurityDomain
import pt.isel.mem.TransactionManagerInMem
import pt.isel.repositories.jdbi.TransactionManagerJdbi
import pt.isel.repositories.jdbi.configureWithAppRequirements
import java.time.LocalDateTime
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class ChannelServiceTest {
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

    private fun createSecondUser(
        creatorId: Long,
        channelId: Long,
        userService: UserService,
        trxManager: TransactionManager,
    ): User {
        val invitationService = InvitationService(trxManager)
        val invitation =
            invitationService.createInvitation(
                creatorId,
                channelId,
                AccessType.READ_WRITE,
                LocalDateTime.now().plusDays(7),
            )
        assertTrue(invitation is Either.Right)
        val user2 = userService.registerUser("username2", VALID_PASSWORD, invitation.value.token)
        assertTrue(user2 is Either.Right)
        return user2.value
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `create a valid channel`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user = userService.registerUser("username", VALID_PASSWORD)
        assertTrue(user is Either.Right)
        val channelService = ChannelService(trxManager)
        val channel = channelService.createChannel("channelName", user.value.id, true)
        assertTrue(channel is Either.Right)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `create a channel with empty name`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user = userService.registerUser("username", VALID_PASSWORD)
        assertTrue(user is Either.Right)
        val channelService = ChannelService(trxManager)
        val channel = channelService.createChannel("", user.value.id, true)
        assertTrue(channel is Either.Left)
        assertEquals(ChannelError.EmptyChannelName, channel.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `create a channel with an already existing name`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user = userService.registerUser("username", VALID_PASSWORD)
        assertTrue(user is Either.Right)
        val channelService = ChannelService(trxManager)
        val channel = channelService.createChannel("channelName", user.value.id, true)
        assertTrue(channel is Either.Right)
        val channel2 = channelService.createChannel("channelName", user.value.id, true)
        assertTrue(channel2 is Either.Left)
        assertEquals(ChannelError.ChannelAlreadyExists, channel2.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `join a public channel`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user1 = userService.registerUser("username", VALID_PASSWORD)
        assertTrue(user1 is Either.Right)
        val channelService = ChannelService(trxManager)
        val channel = channelService.createChannel("channelName", user1.value.id, false)
        assertTrue(channel is Either.Right)
        val user2 = createSecondUser(user1.value.id, channel.value.id, userService, trxManager)
        val channel2 = channelService.createChannel("public", user1.value.id, true)
        assertTrue(channel2 is Either.Right)
        val join = channelService.joinPublicChannel(user2.id, channel2.value.id)
        assertTrue(join is Either.Right)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `owner joins his own channel`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user = userService.registerUser("username", VALID_PASSWORD)
        assertTrue(user is Either.Right)
        val channelService = ChannelService(trxManager)
        val channel = channelService.createChannel("channelName", user.value.id, true)
        assertTrue(channel is Either.Right)
        val join = channelService.joinPublicChannel(user.value.id, channel.value.id)
        assertTrue(join is Either.Left)
    }

    /*
    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `join a private channel with invalid invitation`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, pt.isel.services.TestClock())
        val user1 = userService.registerUser("username1", pt.isel.services.VALID_PASSWORD)
        assertTrue(user1 is Either.Right)
        val channelService = ChannelService(trxManager)
        val channel = channelService.createChannel("channelName", user1.value.id, false)
        assertTrue(channel is Either.Right)
        val user2 = createSecondUser(user1.value.id, channel.value.id, userService, trxManager)
        val channel2 = channelService.createChannel("private", user1.value.id, false)
        assertTrue(channel2 is Either.Right)
        val join = channelService.joinChannel(user2.id, channel2.value.id)
        assertTrue(join is Either.Left)
        assertEquals(ChannelError.EmptyToken, join.value)
    }
     */

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `leave a channel`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user1 = userService.registerUser("username1", VALID_PASSWORD)
        assertTrue(user1 is Either.Right)
        val channelService = ChannelService(trxManager)
        val channel = channelService.createChannel("channelName", user1.value.id, false)
        assertTrue(channel is Either.Right)
        val user2 = createSecondUser(user1.value.id, channel.value.id, userService, trxManager)
        val leave = channelService.leaveChannel(channel.value.id, user2.id)
        assertTrue(leave is Either.Right)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `leave a channel that user is not in`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user1 = userService.registerUser("username1", VALID_PASSWORD)
        assertTrue(user1 is Either.Right)
        val channelService = ChannelService(trxManager)
        val channel = channelService.createChannel("channelName", user1.value.id, false)
        assertTrue(channel is Either.Right)
        val user2 = createSecondUser(user1.value.id, channel.value.id, userService, trxManager)
        val channel2 = channelService.createChannel("private", user1.value.id, false)
        assertTrue(channel2 is Either.Right)
        val leave = channelService.leaveChannel(channel2.value.id, user2.id)
        assertTrue(leave is Either.Left)
        print(leave.value)
        assertEquals(ChannelError.UserNotInChannel, leave.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `leave a channel that does not exist`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user = userService.registerUser("username1", VALID_PASSWORD)
        assertTrue(user is Either.Right)
        val channelService = ChannelService(trxManager)
        val leave = channelService.leaveChannel(1, user.value.id)
        assertTrue(leave is Either.Left)
        assertEquals(ChannelError.ChannelNotFound, leave.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `owner leaves a channel`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user = userService.registerUser("username", VALID_PASSWORD)
        assertTrue(user is Either.Right)
        val channelService = ChannelService(trxManager)
        val channel = channelService.createChannel("channelName", user.value.id, true)
        assertTrue(channel is Either.Right)
        val leave = channelService.leaveChannel(channel.value.id, user.value.id)
        assertTrue(leave is Either.Left)
        assertEquals(ChannelError.OwnerCannotLeave, leave.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `get joined channels as a owner`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user = userService.registerUser("username", VALID_PASSWORD)
        assertTrue(user is Either.Right)
        val channelService = ChannelService(trxManager)
        val channel = channelService.createChannel("channelName", user.value.id, true)
        assertTrue(channel is Either.Right)
        val joinedChannels = channelService.getJoinedChannels(user.value.id)
        assertTrue(joinedChannels is Either.Right)
        assertEquals(1, joinedChannels.value.size)
        assertEquals(listOf(channel.value), joinedChannels.value)
    }

    /*
    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `get joined channels as a user`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, pt.isel.services.TestClock())
        val user1 = userService.createUser("username1", pt.isel.services.VALID_PASSWORD)
        val user2 = userService.createUser("username2", pt.isel.services.VALID_PASSWORD)
        assertTrue(user1 is Either.Right)
        assertTrue(user2 is Either.Right)
        val channelService = ChannelService(trxManager)
        val channel = channelService.createChannel("channelName", user1.value.id, true)
        val channel2 = channelService.createChannel("channelName2", user2.value.id, true)
        assertTrue(channel is Either.Right)
        assertTrue(channel2 is Either.Right)
        val join = channelService.joinChannel(user2.value.id, channel.value.id)
        assertTrue(join is Either.Right)
        val joinedChannels = channelService.getJoinedChannels(user2.value.id)
        assertTrue(joinedChannels is Either.Right)
        assertEquals(2, joinedChannels.value.size)
        assertTrue(joinedChannels.value.containsAll(listOf(channel.value, channel2.value)))
    }
     */

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `search channels`(trxManager: TransactionManager) {
        val userService = createUserService(trxManager, TestClock())
        val user = userService.registerUser("username", VALID_PASSWORD)
        assertTrue(user is Either.Right)
        val channelService = ChannelService(trxManager)
        val channel = channelService.createChannel("channelName", user.value.id, true)
        assertTrue(channel is Either.Right)
        val search = channelService.searchChannels("channel")
        assertTrue(search is Either.Right)
    }
}
