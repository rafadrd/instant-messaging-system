package pt.isel.repositories.jdbi

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.BeforeEach
import org.postgresql.ds.PGSimpleDataSource
import pt.isel.domain.security.PasswordValidationInfo
import pt.isel.domain.users.User
import pt.isel.repositories.jdbi.channels.ChannelRepositoryJdbi
import pt.isel.repositories.jdbi.users.UserRepositoryJdbi
import pt.isel.repositories.jdbi.utils.configureWithAppRequirements
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChannelRepositoryJdbiTest {
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

    @Test
    fun `create a new channel`() {
        runWithHandle { handle ->
            val channelRepo = ChannelRepositoryJdbi(handle)
            val user = createUser()
            val channel = channelRepo.create("channel", user, true)

            assertEquals("channel", channel.name)
            assertEquals(user.id, channel.owner.id)
            assertEquals(true, channel.isPublic)
        }
    }

    @Test
    fun `create a channel with duplicate name`() {
        runWithHandle { handle ->
            val channelRepo = ChannelRepositoryJdbi(handle)
            val user = createUser()
            channelRepo.create("channel", user, true)

            assertFailsWith<Exception> { channelRepo.create("channel", user, false) }
        }
    }

    @Test
    fun `find an existing channel by ID`() {
        runWithHandle { handle ->
            val channelRepo = ChannelRepositoryJdbi(handle)
            val user = createUser()
            val createdChannel = channelRepo.create("channel", user, true)
            val foundChannel = channelRepo.findById(createdChannel.id)

            assertEquals(createdChannel, foundChannel)
        }
    }

    @Test
    fun `find a non-existent channel by ID`() {
        runWithHandle { handle ->
            val channelRepo = ChannelRepositoryJdbi(handle)

            val foundChannel = channelRepo.findById(Long.MAX_VALUE)
            assertNull(foundChannel)
        }
    }

    @Test
    fun `find a channel by name`() {
        runWithHandle { handle ->
            val channelRepo = ChannelRepositoryJdbi(handle)
            val user = createUser()
            val createdChannel = channelRepo.create("channel", user, true)
            val foundChannel = channelRepo.findByName("channel")

            assertEquals(createdChannel, foundChannel)
        }
    }

    @Test
    fun `find a non-existent channel by name`() {
        runWithHandle { handle ->
            val channelRepo = ChannelRepositoryJdbi(handle)

            val foundChannel = channelRepo.findByName("not-channel")
            assertNull(foundChannel)
        }
    }

    @Test
    fun `retrieve all public channels`() {
        runWithHandle { handle ->
            val channelRepo = ChannelRepositoryJdbi(handle)
            val user = createUser()

            channelRepo.create("public-channel1", user, true)
            channelRepo.create("public-channel2", user, true)
            channelRepo.create("private-channel", user, false)

            val publicChannels = channelRepo.findAllPublicChannels(3, 0)
            assertEquals(2, publicChannels.size)
            assertTrue(publicChannels.all { it.isPublic })
        }
    }

    @Test
    fun `retrieve all channels owned by a specific user`() {
        runWithHandle { handle ->
            val channelRepo = ChannelRepositoryJdbi(handle)
            val user1 = createUser("user1")
            val user2 = createUser("user2")
            channelRepo.create("user1-channel1", user1, true)
            channelRepo.create("user1-channel2", user1, true)
            channelRepo.create("user2-channel", user2, true)

            val user1Channels = channelRepo.findAllByOwner(user1)
            assertEquals(2, user1Channels.size)
            assertTrue(user1Channels.all { it.owner.id == user1.id })
        }
    }

    @Test
    fun `retrieve all channels`() {
        runWithHandle { handle ->
            val channelRepo = ChannelRepositoryJdbi(handle)
            val user = createUser()
            channelRepo.create("channel1", user, true)
            channelRepo.create("channel2", user, false)

            val allChannels = channelRepo.findAll()
            assertEquals(2, allChannels.size)
        }
    }

    @Test
    fun `update an existing channel`() {
        runWithHandle { handle ->
            val channelRepo = ChannelRepositoryJdbi(handle)
            val user = createUser()
            val channel = channelRepo.create("channel", user, true)
            channelRepo.save(channel.copy(name = "updated-channel", isPublic = false))

            val updatedChannel = channelRepo.findById(channel.id)
            assertEquals("updated-channel", updatedChannel?.name)
            assertEquals(false, updatedChannel?.isPublic)
        }
    }

    @Test
    fun `delete a channel by ID`() {
        runWithHandle { handle ->
            val channelRepo = ChannelRepositoryJdbi(handle)
            val user = createUser()
            val channel = channelRepo.create("channel", user, true)
            channelRepo.deleteById(channel.id)

            val deletedChannel = channelRepo.findById(channel.id)
            assertNull(deletedChannel)
        }
    }

    @Test
    fun `test Remove all channels`() {
        runWithHandle { handle ->
            val channelRepo = ChannelRepositoryJdbi(handle)
            val user = createUser()

            channelRepo.create("channel1", user, true)
            channelRepo.create("channel2", user, false)

            channelRepo.clear()

            val allChannels = channelRepo.findAll()
            assertTrue(allChannels.isEmpty())
        }
    }
}
