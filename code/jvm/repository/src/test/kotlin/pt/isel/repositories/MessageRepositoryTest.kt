package pt.isel.repositories

import org.junit.jupiter.api.Assertions.assertNull
import pt.isel.domain.Channel
import pt.isel.domain.Message
import pt.isel.domain.User
import pt.isel.domain.auth.PasswordValidationInfo
import pt.isel.mem.MessageRepositoryInMem
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageRepositoryTest {
    private val messageRepository = MessageRepositoryInMem()

    private val user = User(1, "AntonioBanderas", PasswordValidationInfo(PASSWORD))
    private val user2 = User(2, "Catherine", PasswordValidationInfo(PASSWORD))
    private val channel = Channel(1, "Zorro", user)

    // Testing the creation of messages
    private val message = messageRepository.create("Hello", user, channel)
    private val message2 = messageRepository.create("Hi", user2, channel)

    @Test
    fun `Find message by id`() {
        val result = messageRepository.findById(message.id)
        assertEquals(message, result)
    }

    @Test
    fun `Find all messages in channel`() {
        val result = messageRepository.findAllInChannel(channel, 20, 0)
        assertEquals(listOf(message, message2), result)
    }

    @Test
    fun `Save message`() {
        val newMessage = Message(message.id, "Updated Hello", user, channel)
        messageRepository.save(newMessage)
        val result = messageRepository.findById(message.id)
        assertEquals(newMessage, result)
    }

    @Test
    fun `Delete message by id`() {
        messageRepository.deleteById(message.id)
        val result = messageRepository.findById(message.id)
        assertNull(result)
    }

    @Test
    fun `Clear all messages`() {
        messageRepository.clear()
        val result = messageRepository.findAll()
        assertEquals(emptyList(), result)
    }

    @Test
    fun `Find all messages`() {
        val result = messageRepository.findAll()
        assertEquals(listOf(message, message2), result)
    }
}
