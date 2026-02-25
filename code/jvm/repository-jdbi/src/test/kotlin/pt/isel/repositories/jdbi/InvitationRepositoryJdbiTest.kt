package pt.isel.repositories.jdbi

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.BeforeEach
import org.postgresql.ds.PGSimpleDataSource
import pt.isel.domain.AccessType
import pt.isel.domain.Channel
import pt.isel.domain.Status
import pt.isel.domain.User
import pt.isel.domain.auth.PasswordValidationInfo
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InvitationRepositoryJdbiTest {
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
            InvitationRepositoryJdbi(handle).clear()
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

    private fun newTokenValidationData() = "token-${abs(Random.nextLong())}"

    @Test
    fun `Test Successful Invitation Creation`() {
        runWithHandle { handle ->
            val invitationRepo = InvitationRepositoryJdbi(handle)
            val user = createUser("user1")
            val channel = createChannel("channel1", user)
            val token = newTokenValidationData()
            val expiresAt = LocalDateTime.now().plusDays(7)

            val invitation =
                invitationRepo.create(
                    token = token,
                    createdBy = user,
                    channel = channel,
                    accessType = AccessType.READ_WRITE,
                    expiresAt = expiresAt,
                )

            assertEquals(channel.id, invitation.channel.id)
            assertEquals(user.id, invitation.createdBy.id)
            assertEquals(AccessType.READ_WRITE, invitation.accessType)
            assertEquals(token, invitation.token)
            assertEquals(expiresAt.truncatedTo(ChronoUnit.MILLIS), invitation.expiresAt)
        }
    }

    @Test
    fun `Test Invitation Creation with Duplicate Token`() {
        runWithHandle { handle ->
            val invitationRepo = InvitationRepositoryJdbi(handle)
            val user = createUser()
            val channel = createChannel("channel", user)
            val token = newTokenValidationData()
            val expiresAt = LocalDateTime.now().plusDays(7)

            invitationRepo.create(
                token = token,
                createdBy = user,
                channel = channel,
                accessType = AccessType.READ_ONLY,
                expiresAt = expiresAt,
            )

            assertFailsWith<Exception> {
                invitationRepo.create(
                    token = token,
                    createdBy = user,
                    channel = channel,
                    accessType = AccessType.READ_ONLY,
                    expiresAt = expiresAt,
                )
            }
        }
    }

    @Test
    fun `Test Finding Existing Invitation by ID`() {
        runWithHandle { handle ->
            val invitationRepo = InvitationRepositoryJdbi(handle)
            val user = createUser()
            val channel = createChannel("channel", user)

            val token = newTokenValidationData()
            val expiresAt = LocalDateTime.now().plusDays(7)

            val createdInvitation =
                invitationRepo.create(
                    token = token,
                    createdBy = user,
                    channel = channel,
                    accessType = AccessType.READ_WRITE,
                    expiresAt = expiresAt,
                )

            val foundInvitation = invitationRepo.findById(createdInvitation.id)
            assertEquals(createdInvitation, foundInvitation)
        }
    }

    @Test
    fun `Test Finding Non-existent Invitation by ID`() {
        runWithHandle { handle ->
            val invitationRepo = InvitationRepositoryJdbi(handle)

            val foundInvitation = invitationRepo.findById(Long.MAX_VALUE)
            assertNull(foundInvitation)
        }
    }

    @Test
    fun `Test Finding Existing Invitation by Token`() {
        runWithHandle { handle ->
            val invitationRepo = InvitationRepositoryJdbi(handle)
            val user = createUser()
            val channel = createChannel("channel", user)

            val token = newTokenValidationData()
            val expiresAt = LocalDateTime.now().plusDays(7)

            val createdInvitation =
                invitationRepo.create(
                    token = token,
                    createdBy = user,
                    channel = channel,
                    accessType = AccessType.READ_WRITE,
                    expiresAt = expiresAt,
                )

            val foundInvitation = invitationRepo.findByToken(token)
            assertEquals(createdInvitation, foundInvitation)
        }
    }

    @Test
    fun `Test Finding Non-existent Invitation by Token`() {
        runWithHandle { handle ->
            val invitationRepo = InvitationRepositoryJdbi(handle)

            val foundInvitation = invitationRepo.findByToken("not-token")
            assertNull(foundInvitation)
        }
    }

    @Test
    fun `Test Retrieve All Invitations`() {
        runWithHandle { handle ->
            val invitationRepo = InvitationRepositoryJdbi(handle)
            val user = createUser()
            val channel = createChannel("channel", user)

            val token1 = newTokenValidationData()
            val token2 = newTokenValidationData()
            val expiresAt = LocalDateTime.now().plusDays(7)

            invitationRepo.create(
                token = token1,
                createdBy = user,
                channel = channel,
                accessType = AccessType.READ_WRITE,
                expiresAt = expiresAt,
            )

            invitationRepo.create(
                token = token2,
                createdBy = user,
                channel = channel,
                accessType = AccessType.READ_WRITE,
                expiresAt = expiresAt,
            )

            val allInvitations = invitationRepo.findAll()
            assertEquals(2, allInvitations.size)
        }
    }

    @Test
    fun `Test Successful Invitation Update`() {
        runWithHandle { handle ->
            val invitationRepo = InvitationRepositoryJdbi(handle)
            val user = createUser()
            val channel = createChannel("channel", user)

            val token = newTokenValidationData()
            val expiresAt = LocalDateTime.now().plusDays(7)

            val invitation =
                invitationRepo.create(
                    token = token,
                    createdBy = user,
                    channel = channel,
                    accessType = AccessType.READ_ONLY,
                    expiresAt = expiresAt,
                )

            invitationRepo.save(
                invitation.copy(status = Status.REJECTED),
            )

            val updatedInvitation = invitationRepo.findById(invitation.id)
            assertEquals(Status.REJECTED, updatedInvitation?.status)
        }
    }

    @Test
    fun `Test Successful Invitation Deletion by ID`() {
        runWithHandle { handle ->
            val invitationRepo = InvitationRepositoryJdbi(handle)
            val user = createUser("user")
            val channel = createChannel("channel", user)

            val token = newTokenValidationData()
            val expiresAt = LocalDateTime.now().plusDays(7)

            val invitation =
                invitationRepo.create(
                    token = token,
                    createdBy = user,
                    channel = channel,
                    accessType = AccessType.READ_WRITE,
                    expiresAt = expiresAt,
                )

            invitationRepo.deleteById(invitation.id)

            val deletedInvitation = invitationRepo.findById(invitation.id)
            assertNull(deletedInvitation)
        }
    }

    @Test
    fun `Test Clear All Invitations`() {
        runWithHandle { handle ->
            val invitationRepo = InvitationRepositoryJdbi(handle)
            val user = createUser()
            val channel = createChannel("channel", user)

            val token1 = newTokenValidationData()
            val token2 = newTokenValidationData()
            val expiresAt = LocalDateTime.now().plusDays(7)

            invitationRepo.create(
                token = token1,
                createdBy = user,
                channel = channel,
                accessType = AccessType.READ_ONLY,
                expiresAt = expiresAt,
            )

            invitationRepo.create(
                token = token2,
                createdBy = user,
                channel = channel,
                accessType = AccessType.READ_WRITE,
                expiresAt = expiresAt,
            )

            invitationRepo.clear()

            val allInvitations = invitationRepo.findAll()
            assertTrue(allInvitations.isEmpty())
        }
    }
}
