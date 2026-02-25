package pt.isel.repositories.jdbi

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.BeforeEach
import org.postgresql.ds.PGSimpleDataSource
import pt.isel.domain.Channel
import pt.isel.domain.User
import pt.isel.domain.auth.PasswordValidationInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MessageRepositoryJdbiTest {
    companion object {
        private fun runWithHandle(block: (Handle) -> Unit) = jdbi.useTransaction<Exception>(block)

        private val jdbi =
            Jdbi
                .create(PGSimpleDataSource().apply { setURL(Environment.getDbUrl()) })
                .configureWithAppRequirements()
    }

    @BeforeEach
    fun clean() {
        runWithHandle { handle: Handle ->
            MessageRepositoryJdbi(handle).clear()
            ChannelRepositoryJdbi(handle).clear()
            UserRepositoryJdbi(handle).clear()
        }
    }

    private fun createUser(
        username: String = "user",
        password: String = "password",
    ): User {
        val userRepo = UserRepositoryJdbi(jdbi.open())
        return userRepo.create(username, PasswordValidationInfo(password))
    }

    private fun createChannel(
        name: String,
        owner: User,
        isPublic: Boolean = true,
    ): Channel {
        val channelRepo = ChannelRepositoryJdbi(jdbi.open())
        return channelRepo.create(name, owner, isPublic)
    }

    @Test
    fun `create a new message`() {
        runWithHandle { handle ->
            val messageRepo = MessageRepositoryJdbi(handle)
            val user = createUser()
            val channel = createChannel("channel", user)

            val message = messageRepo.create("ola", user, channel)

            assertEquals("ola", message.content)
            assertEquals(user.id, message.user.id)
            assertEquals(channel.id, message.channel.id)
            assertTrue(message.id > 0)
        }
    }

    @Test
    fun `create a empty message`() {
        runWithHandle { handle ->
            val messageRepo = MessageRepositoryJdbi(handle)
            val user = createUser()
            val channel = createChannel("channel", user)

            assertFailsWith<Exception> { messageRepo.create("", user, channel) }
        }
    }

    @Test
    fun `find an existing message by ID`() {
        runWithHandle { handle ->
            val messageRepo = MessageRepositoryJdbi(handle)
            val user = createUser()
            val channel = createChannel("channel", user)

            val createdMessage = messageRepo.create("ola", user, channel)
            val foundMessage = messageRepo.findById(createdMessage.id)

            assertEquals(createdMessage, foundMessage)
        }
    }

    @Test
    fun `find an non-existing message by ID`() {
        runWithHandle { handle ->
            val messageRepo = MessageRepositoryJdbi(handle)

            val foundMessage = messageRepo.findById(Long.MAX_VALUE)
            assertNull(foundMessage)
        }
    }

    @Test
    fun `find paginated results in channel`() {
        runWithHandle { handle ->
            val messageRepo = MessageRepositoryJdbi(handle)
            val user = createUser()
            val channel = createChannel("channel", user)

            messageRepo.create("message1", user, channel)
            messageRepo.create("message2", user, channel)
            messageRepo.create("message3", user, channel)

            val messages = messageRepo.findAllInChannel(channel, limit = 2, offset = 1)

            assertEquals(2, messages.size)
            assertEquals("message2", messages[0].content)
            assertEquals("message3", messages[1].content)
        }
    }

    @Test
    fun `find empty list in channel`() {
        runWithHandle { handle ->
            val messageRepo = MessageRepositoryJdbi(handle)
            val user = createUser()
            val channel = createChannel("channel", user)

            val messages = messageRepo.findAllInChannel(channel, limit = 10, offset = 0)
            assertTrue(messages.isEmpty())
        }
    }

    @Test
    fun `retrieve all messages`() {
        runWithHandle { handle ->
            val messageRepo = MessageRepositoryJdbi(handle)
            val user = createUser()
            val channel = createChannel("channel", user)

            messageRepo.create("message1", user, channel)
            messageRepo.create("message2", user, channel)

            val allMessages = messageRepo.findAll()
            assertEquals(2, allMessages.size)
        }
    }

    @Test
    fun `empty result for messages`() {
        runWithHandle { handle ->
            val messageRepo = MessageRepositoryJdbi(handle)

            val allMessages = messageRepo.findAll()
            assertTrue(allMessages.isEmpty())
        }
    }

    @Test
    fun `update message`() {
        runWithHandle { handle ->
            val messageRepo = MessageRepositoryJdbi(handle)
            val user = createUser()
            val channel = createChannel("channel", user)

            val message = messageRepo.create("ola", user, channel)
            messageRepo.save(message.copy(content = "novo ola"))

            val updatedMessage = messageRepo.findById(message.id)
            assertEquals("novo ola", updatedMessage?.content)
        }
    }

    @Test
    fun `update message with empty`() {
        runWithHandle { handle ->
            val messageRepo = MessageRepositoryJdbi(handle)
            val user = createUser()
            val channel = createChannel("channel", user)

            val message = messageRepo.create("ola", user, channel)

            assertFailsWith<Exception> { messageRepo.save(message.copy(content = "")) }
        }
    }

    @Test
    fun `delete a message by ID`() {
        runWithHandle { handle ->
            val messageRepo = MessageRepositoryJdbi(handle)
            val user = createUser()
            val channel = createChannel("channel", user)

            val message = messageRepo.create("ola", user, channel)
            messageRepo.deleteById(message.id)

            val deletedMessage = messageRepo.findById(message.id)
            assertNull(deletedMessage)
        }
    }

    @Test
    fun `clear all messages`() {
        runWithHandle { handle ->
            val messageRepo = MessageRepositoryJdbi(handle)
            val user = createUser()

            messageRepo.create("message1", user, createChannel("channel1", user))
            messageRepo.create("message2", user, createChannel("channel2", user))

            messageRepo.clear()

            val allMessages = messageRepo.findAll()
            assertTrue(allMessages.isEmpty())
        }
    }
}
