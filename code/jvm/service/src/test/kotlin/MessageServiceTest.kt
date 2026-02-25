package pt.isel

import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pt.isel.auth.PasswordSecurityDomain
import pt.isel.auth.Sha256TokenEncoder
import pt.isel.auth.UsersDomainConfig
import pt.isel.mem.TransactionManagerInMem
import java.time.LocalDateTime
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class MessageServiceTest {
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
                AccessType.READ_ONLY,
                LocalDateTime.now().plusDays(7),
            )
        assertTrue(invitation is Either.Right)
        val user2 = userService.registerUser("username2", VALID_PASSWORD, invitation.value.token)
        assertTrue(user2 is Either.Right)
        return user2.value
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `Test Create a new message with valid input`(trxManager: TransactionManager) {
        val testClock = TestClock()
        val userService = createUserService(trxManager, testClock)
        val channelService = ChannelService(trxManager)
        val messageEventService = MessageEventService(trxManager)
        val messageService = MessageService(trxManager, messageEventService)
        val user = userService.registerUser("user", VALID_PASSWORD)
        assertTrue(user is Either.Right)
        val channel = channelService.createChannel("channel", user.value.id, true)
        assertTrue(channel is Either.Right)
        val message = messageService.createMessage("message", user.value.id, channel.value.id)
        assertTrue(message is Either.Right)
        assertEquals("message", message.value.content)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `Test Create a message with invalid input`(trxManager: TransactionManager) {
        val testClock = TestClock()
        val userService = createUserService(trxManager, testClock)
        val channelService = ChannelService(trxManager)
        val messageEventService = MessageEventService(trxManager)
        val messageService = MessageService(trxManager, messageEventService)
        val user = userService.registerUser("user", VALID_PASSWORD)
        assertTrue(user is Either.Right)
        val channel = channelService.createChannel("channel", user.value.id, true)
        assertTrue(channel is Either.Right)
        val message = messageService.createMessage("", user.value.id, channel.value.id)
        assertTrue(message is Either.Left)
        assertEquals(MessageError.EmptyMessage, message.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `Test Create a message with invalid user`(trxManager: TransactionManager) {
        val testClock = TestClock()
        val userService = createUserService(trxManager, testClock)
        val channelService = ChannelService(trxManager)
        val messageEventService = MessageEventService(trxManager)
        val messageService = MessageService(trxManager, messageEventService)
        val user = userService.registerUser("user", VALID_PASSWORD)
        assertTrue(user is Either.Right)
        val channel = channelService.createChannel("channel", user.value.id, true)
        assertTrue(channel is Either.Right)
        val message = messageService.createMessage("message", Long.MAX_VALUE, channel.value.id)
        assertTrue(message is Either.Left)
        assertEquals(MessageError.UserNotFound, message.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `Test Create a message with invalid channel`(trxManager: TransactionManager) {
        val testClock = TestClock()
        val userService = createUserService(trxManager, testClock)
        val channelService = ChannelService(trxManager)
        val messageEventService = MessageEventService(trxManager)
        val messageService = MessageService(trxManager, messageEventService)
        val user = userService.registerUser("user", VALID_PASSWORD)
        assertTrue(user is Either.Right)
        val channel = channelService.createChannel("channel", user.value.id, true)
        assertTrue(channel is Either.Right)
        val message = messageService.createMessage("message", user.value.id, Long.MAX_VALUE)
        assertTrue(message is Either.Left)
        assertEquals(MessageError.ChannelNotFound, message.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `Test Create a message with user not in channel`(trxManager: TransactionManager) {
        val testClock = TestClock()
        val userService = createUserService(trxManager, testClock)
        val channelService = ChannelService(trxManager)
        val messageEventService = MessageEventService(trxManager)
        val messageService = MessageService(trxManager, messageEventService)
        val user = userService.registerUser("user", VALID_PASSWORD)
        assertTrue(user is Either.Right)
        val channel = channelService.createChannel("channel", user.value.id, false)
        assertTrue(channel is Either.Right)
        val user2 = createSecondUser(user.value.id, channel.value.id, userService, trxManager)
        val channel2 = channelService.createChannel("private", user.value.id, false)
        assertTrue(channel2 is Either.Right)
        val message = messageService.createMessage("message", user2.id, channel2.value.id)
        assertTrue(message is Either.Left)
        assertEquals(MessageError.UserNotInChannel, message.value)
    }

    /*
    //Verificar se devemos modificar o joinChannel para ter um tipo de Acesso
        @ParameterizedTest
        @MethodSource("transactionManagers")
        fun `Test Create a message with write permission denied`(trxManager: TransactionManager) {
            val testClock = TestClock()
            val userService = createUserService(trxManager, testClock)
            val channelService = ChannelService(trxManager)
            val messageService = MessageService(trxManager)
            val user = userService.createUser("user", VALID_PASSWORD)
            val user2 = userService.createUser("user2", VALID_PASSWORD)
            assertTrue(user is Either.Right)
            assertTrue(user2 is Either.Right)
            val channel = channelService.createChannel("channel", user.value.id, true)
            assertTrue(channel is Either.Right)
            val joinChannel = channelService.joinChannel(user2.value.id, channel.value.id)
            assertTrue(joinChannel is Either.Right)
            // Change user2 access type to READ_ONLY
            val message = messageService.createMessage("message", user2.value.id , channel.value.id)
            assertTrue(message is Either.Left)
            assertEquals(MessageError.WritePermissionDenied, message.value)
        }*/

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `Get messages in channel`(trxManager: TransactionManager) {
        val testClock = TestClock()
        val userService = createUserService(trxManager, testClock)
        val channelService = ChannelService(trxManager)
        val messageEventService = MessageEventService(trxManager)
        val messageService = MessageService(trxManager, messageEventService)
        val user = userService.registerUser("user", VALID_PASSWORD)
        assertTrue(user is Either.Right)
        val channel = channelService.createChannel("channel", user.value.id, true)
        assertTrue(channel is Either.Right)
        val message = messageService.createMessage("message", user.value.id, channel.value.id)
        assertTrue(message is Either.Right)
        val messages = messageService.getMessagesInChannel(user.value.id, channel.value.id)
        assertTrue(messages is Either.Right)
        assertEquals(1, messages.value.size)
        assertEquals("message", messages.value[0].content)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `Get messages in channel  where there are none`(trxManager: TransactionManager) {
        val testClock = TestClock()
        val userService = createUserService(trxManager, testClock)
        val channelService = ChannelService(trxManager)
        val messageEventService = MessageEventService(trxManager)
        val messageService = MessageService(trxManager, messageEventService)
        val user = userService.registerUser("user", VALID_PASSWORD)
        assertTrue(user is Either.Right)
        val channel = channelService.createChannel("channel", user.value.id, true)
        assertTrue(channel is Either.Right)
        val messages = messageService.getMessagesInChannel(user.value.id, channel.value.id)
        assertTrue(messages is Either.Right)
        assertEquals(messages.value, emptyList())
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `Get more than one message in channel`(trxManager: TransactionManager) {
        val testClock = TestClock()
        val userService = createUserService(trxManager, testClock)
        val channelService = ChannelService(trxManager)
        val messageEventService = MessageEventService(trxManager)
        val messageService = MessageService(trxManager, messageEventService)
        val user = userService.registerUser("user", VALID_PASSWORD)
        assertTrue(user is Either.Right)
        val channel = channelService.createChannel("channel", user.value.id, true)
        assertTrue(channel is Either.Right)
        val message = messageService.createMessage("message", user.value.id, channel.value.id)
        assertTrue(message is Either.Right)
        val message2 = messageService.createMessage("message2", user.value.id, channel.value.id)
        assertTrue(message2 is Either.Right)
        val messages = messageService.getMessagesInChannel(user.value.id, channel.value.id)
        assertTrue(messages is Either.Right)
        assertEquals(2, messages.value.size)
        assertEquals("message", messages.value[0].content)
        assertEquals("message2", messages.value[1].content)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `Get messages in channel with invalid limit`(trxManager: TransactionManager) {
        val testClock = TestClock()
        val userService = createUserService(trxManager, testClock)
        val channelService = ChannelService(trxManager)
        val messageEventService = MessageEventService(trxManager)
        val messageService = MessageService(trxManager, messageEventService)
        val user = userService.registerUser("user", VALID_PASSWORD)
        assertTrue(user is Either.Right)
        val channel = channelService.createChannel("channel", user.value.id, true)
        assertTrue(channel is Either.Right)
        val message = messageService.createMessage("message", user.value.id, channel.value.id)
        assertTrue(message is Either.Right)
        val messages = messageService.getMessagesInChannel(user.value.id, channel.value.id, -1)
        assertTrue(messages is Either.Left)
        assertEquals(MessageError.InvalidLimit, messages.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `Get messages in channel with invalid offset`(trxManager: TransactionManager) {
        val testClock = TestClock()
        val userService = createUserService(trxManager, testClock)
        val channelService = ChannelService(trxManager)
        val messageEventService = MessageEventService(trxManager)
        val messageService = MessageService(trxManager, messageEventService)
        val user = userService.registerUser("user", VALID_PASSWORD)
        assertTrue(user is Either.Right)
        val channel = channelService.createChannel("channel", user.value.id, true)
        assertTrue(channel is Either.Right)
        val message = messageService.createMessage("message", user.value.id, channel.value.id)
        assertTrue(message is Either.Right)
        val messages = messageService.getMessagesInChannel(user.value.id, channel.value.id, 1, -1)
        assertTrue(messages is Either.Left)
        assertEquals(MessageError.InvalidOffset, messages.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `Get messages in channel with invalid channel id`(trxManager: TransactionManager) {
        val messageEventService = MessageEventService(trxManager)
        val messageService = MessageService(trxManager, messageEventService)
        val testClock = TestClock()
        val userService = createUserService(trxManager, testClock)
        val user = userService.registerUser("user", VALID_PASSWORD)
        assertTrue(user is Either.Right)
        val messages = messageService.getMessagesInChannel(user.value.id, Long.MAX_VALUE)
        assertTrue(messages is Either.Left)
        assertEquals(MessageError.ChannelNotFound, messages.value)
    }
}
