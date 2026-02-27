package pt.isel.repositories

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import pt.isel.domain.channel.AccessType
import pt.isel.domain.channel.Channel
import pt.isel.domain.security.PasswordValidationInfo
import pt.isel.domain.user.User
import pt.isel.mem.InvitationRepositoryInMem
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class InvitationRepositoryTest {
    private lateinit var repo: InvitationRepositoryInMem
    private lateinit var user: User
    private lateinit var channel: Channel
    private val token = "Axhkwuiwmc296"

    @BeforeEach
    fun setUp() {
        repo = InvitationRepositoryInMem()
        user = User(1, "AntonioBanderas", PasswordValidationInfo(PASSWORD))
        channel = Channel(1, "Channel1", user, true)
    }

    @Test
    fun `create invitation`() {
        val invitation =
            repo.create(token, user, channel, AccessType.READ_ONLY, LocalDateTime.now())
        assertEquals(token, invitation.token)
        assertEquals(user, invitation.createdBy)
        assertEquals(channel, invitation.channel)
        assertEquals(AccessType.READ_ONLY, invitation.accessType)
    }

    @Test
    fun `find by id`() {
        val invitation =
            repo.create(token, user, channel, AccessType.READ_ONLY, LocalDateTime.now())
        val foundInvitation = repo.findById(invitation.id)
        assertNotNull(foundInvitation)
        assertEquals(invitation, foundInvitation)
    }

    @Test
    fun `find by token`() {
        val invitation =
            repo.create(token, user, channel, AccessType.READ_ONLY, LocalDateTime.now())
        val foundInvitation = repo.findByToken(token)
        assertNotNull(foundInvitation)
        assertEquals(invitation, foundInvitation)
    }

    @Test
    fun `find all invitations`() {
        val invitation1 =
            repo.create(token, user, channel, AccessType.READ_ONLY, LocalDateTime.now())
        val invitation2 =
            repo.create("AnotherToken", user, channel, AccessType.READ_ONLY, LocalDateTime.now())
        val invitations = repo.findAll()
        assertEquals(2, invitations.size)
        assertTrue(invitations.contains(invitation1))
        assertTrue(invitations.contains(invitation2))
    }

    @Test
    fun `save invitation`() {
        val invitation =
            repo.create(token, user, channel, AccessType.READ_ONLY, LocalDateTime.now())
        val updatedInvitation = invitation.copy(token = "UpdatedToken")
        repo.save(updatedInvitation)
        val foundInvitation = repo.findById(invitation.id)
        assertNotNull(foundInvitation)
        assertEquals("UpdatedToken", foundInvitation?.token)
    }

    @Test
    fun `delete by id`() {
        val invitation =
            repo.create(token, user, channel, AccessType.READ_ONLY, LocalDateTime.now())
        repo.deleteById(invitation.id)
        val foundInvitation = repo.findById(invitation.id)
        assertNull(foundInvitation)
    }

    @Test
    fun `clear all invitations`() {
        repo.create(token, user, channel, AccessType.READ_ONLY, LocalDateTime.now())
        repo.create("AnotherToken", user, channel, AccessType.READ_ONLY, LocalDateTime.now())
        repo.clear()
        val invitations = repo.findAll()
        assertTrue(invitations.isEmpty())
    }
}
