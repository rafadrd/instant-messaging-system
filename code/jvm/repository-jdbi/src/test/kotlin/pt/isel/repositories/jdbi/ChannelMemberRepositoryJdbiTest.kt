package pt.isel.repositories.jdbi

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.BeforeEach
import org.postgresql.ds.PGSimpleDataSource
import pt.isel.domain.AccessType
import pt.isel.domain.Channel
import pt.isel.domain.User
import pt.isel.domain.auth.PasswordValidationInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ChannelMemberRepositoryJdbiTest {
    companion object {
        private fun runWithHandle(block: (Handle) -> Unit) = jdbi.useTransaction<Exception>(block)

        private val jdbi =
            Jdbi
                .create(PGSimpleDataSource().apply { setURL(Environment.getDbUrl()) })
                .configureWithAppRequirements()
    }

    @BeforeEach
    fun clean() {
        runWithHandle { handle ->
            ChannelMemberRepositoryJdbi(handle).clear()
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
    fun `Test Add User to Channel`() {
        runWithHandle { handle ->
            val channelMemberRepo = ChannelMemberRepositoryJdbi(handle)
            val user = createUser()
            val channel = createChannel("channel", user)

            val channelMember =
                channelMemberRepo.addUserToChannel(
                    user = user,
                    channel = channel,
                    accessType = AccessType.READ_WRITE,
                )

            assertEquals(channel.id, channelMember.channel.id)
            assertEquals(user.id, channelMember.user.id)
            assertEquals(AccessType.READ_WRITE, channelMember.accessType)
        }
    }

    @Test
    fun `Test Add User to Channel with Invalid User or Channel`() {
        runWithHandle { handle ->
            val channelMemberRepo = ChannelMemberRepositoryJdbi(handle)

            val user = User(Long.MAX_VALUE, "user", PasswordValidationInfo("hash"))
            val invalidChannel = Channel(Long.MAX_VALUE, "channel", user)

            assertFailsWith<Exception> {
                channelMemberRepo.addUserToChannel(
                    user = user,
                    channel = invalidChannel,
                    accessType = AccessType.READ_ONLY,
                )
            }
        }
    }

    @Test
    fun `Test Find Existing ChannelMember by ID`() {
        runWithHandle { handle ->
            val channelMemberRepo = ChannelMemberRepositoryJdbi(handle)
            val user = createUser()
            val channel = createChannel("channel", user)

            val channelMember =
                channelMemberRepo.addUserToChannel(user, channel, AccessType.READ_WRITE)

            val foundChannelMember = channelMemberRepo.findById(channelMember.id)
            assertEquals(channelMember, foundChannelMember)
        }
    }

    @Test
    fun `Test Find Non-existent ChannelMember by ID`() {
        runWithHandle { handle ->
            val channelMemberRepo = ChannelMemberRepositoryJdbi(handle)

            val foundChannelMember = channelMemberRepo.findById(Long.MAX_VALUE)
            assertNull(foundChannelMember)
        }
    }

    @Test
    fun `Test Find User in Channel`() {
        runWithHandle { handle ->
            val channelMemberRepo = ChannelMemberRepositoryJdbi(handle)
            val user = createUser()
            val channel = createChannel("channel", user)

            val channelMember =
                channelMemberRepo.addUserToChannel(user, channel, AccessType.READ_ONLY)

            val foundChannelMember = channelMemberRepo.findUserInChannel(user, channel)
            assertEquals(channelMember, foundChannelMember)
        }
    }

    @Test
    fun `Test Find User Not Present in Channel`() {
        runWithHandle { handle ->
            val channelMemberRepo = ChannelMemberRepositoryJdbi(handle)
            val user = createUser()
            val channel = createChannel("channel", user)

            val foundChannelMember = channelMemberRepo.findUserInChannel(user, channel)
            assertNull(foundChannelMember)
        }
    }

    @Test
    fun `Test Find All Channels for User`() {
        runWithHandle { handle ->
            val channelMemberRepo = ChannelMemberRepositoryJdbi(handle)
            val user = createUser()
            val channel1 = createChannel("channel1", user)
            val channel2 = createChannel("channel2", user)

            channelMemberRepo.addUserToChannel(user, channel1, AccessType.READ_WRITE)
            channelMemberRepo.addUserToChannel(user, channel2, AccessType.READ_ONLY)

            val userChannels = channelMemberRepo.findAllChannelsForUser(user, 2, 0)
            assertEquals(2, userChannels.size)
            assertEquals(
                setOf(channel1.id, channel2.id),
                userChannels.map { it.channel.id }.toSet(),
            )
        }
    }

    @Test
    fun `Test Retrieve All ChannelMembers`() {
        runWithHandle { handle ->
            val channelMemberRepo = ChannelMemberRepositoryJdbi(handle)
            val user = createUser()
            val channel = createChannel("channel", user)

            channelMemberRepo.addUserToChannel(user, channel, AccessType.READ_ONLY)

            val allChannelMembers = channelMemberRepo.findAll()
            assertEquals(1, allChannelMembers.size)
        }
    }

    @Test
    fun `Test Update ChannelMember Access Type`() {
        runWithHandle { handle ->
            val channelMemberRepo = ChannelMemberRepositoryJdbi(handle)
            val user = createUser()
            val channel = createChannel("channel", user)

            val channelMember =
                channelMemberRepo.addUserToChannel(user, channel, AccessType.READ_ONLY)

            channelMemberRepo.save(channelMember.copy(accessType = AccessType.READ_WRITE))

            val updatedChannelMember = channelMemberRepo.findById(channelMember.id)
            assertEquals(AccessType.READ_WRITE, updatedChannelMember?.accessType)
        }
    }

    @Test
    fun `Test Remove User from Channel`() {
        runWithHandle { handle ->
            val channelMemberRepo = ChannelMemberRepositoryJdbi(handle)
            val user = createUser()
            val channel = createChannel("channel", user, true)

            channelMemberRepo.addUserToChannel(user, channel, AccessType.READ_ONLY)
            channelMemberRepo.removeUserFromChannel(user, channel)

            val removedChannelMember = channelMemberRepo.findUserInChannel(user, channel)
            assertNull(removedChannelMember)
        }
    }

    @Test
    fun `Test Delete ChannelMember by ID`() {
        runWithHandle { handle ->
            val channelMemberRepo = ChannelMemberRepositoryJdbi(handle)
            val user = createUser()
            val channel = createChannel("channel", user)

            val channelMember =
                channelMemberRepo.addUserToChannel(user, channel, AccessType.READ_WRITE)

            channelMemberRepo.deleteById(channelMember.id)

            val deletedChannelMember = channelMemberRepo.findById(channelMember.id)
            assertNull(deletedChannelMember)
        }
    }

    @Test
    fun `Test Clear All ChannelMembers`() {
        runWithHandle { handle ->
            val channelMemberRepo = ChannelMemberRepositoryJdbi(handle)
            val user = createUser()
            val channel = createChannel("channel", user)

            channelMemberRepo.addUserToChannel(user, channel, AccessType.READ_WRITE)

            channelMemberRepo.clear()

            val allChannelMembers = channelMemberRepo.findAll()
            assertEquals(0, allChannelMembers.size)
        }
    }
}
