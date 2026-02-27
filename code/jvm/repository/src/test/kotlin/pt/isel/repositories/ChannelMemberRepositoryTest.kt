package pt.isel.repositories

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import pt.isel.domain.channels.AccessType
import pt.isel.domain.security.PasswordValidationInfo
import pt.isel.mem.ChannelMemberRepositoryInMem
import pt.isel.mem.ChannelRepositoryInMem
import pt.isel.mem.UserRepositoryInMem
import kotlin.test.Test
import kotlin.test.assertEquals

class ChannelMemberRepositoryTest {
    private lateinit var repoUsers: UserRepositoryInMem
    private lateinit var repoChannels: ChannelRepositoryInMem
    private lateinit var repoMemberships: ChannelMemberRepositoryInMem

    @BeforeEach
    fun setUp() {
        repoUsers =
            UserRepositoryInMem().also {
                it.create("AntonioBanderas", PasswordValidationInfo(PASSWORD))
                it.create("AngelinaJolie", PasswordValidationInfo(PASSWORD))
                it.create("BradPitt", PasswordValidationInfo(PASSWORD))
            }

        repoChannels =
            ChannelRepositoryInMem().also {
                it.create("Channel1", repoUsers.findByUsername("AntonioBanderas")!!, true)
                it.create("Channel2", repoUsers.findByUsername("AngelinaJolie")!!, true)
                it.create("Channel3", repoUsers.findByUsername("BradPitt")!!, true)
            }

        repoMemberships = ChannelMemberRepositoryInMem()
    }

    @Test
    fun `add member to channel`() {
        val channel = repoChannels.findAll().first()
        val user = repoUsers.findByUsername("AntonioBanderas")!!
        repoMemberships.addUserToChannel(user, channel, AccessType.READ_WRITE)
        assertEquals(1, repoMemberships.findAll().size)
        assertEquals(user, repoMemberships.findAll().first().user)
        assertEquals(channel, repoMemberships.findAll().first().channel)
        assertEquals(AccessType.READ_WRITE, repoMemberships.findAll().first().accessType)
    }

    @Test
    fun `remove member from channel`() {
        val channel = repoChannels.findAll().first()
        val user = repoUsers.findByUsername("AntonioBanderas")!!
        repoMemberships.addUserToChannel(user, channel, AccessType.READ_WRITE)
        assertEquals(1, repoMemberships.findAll().size)
        repoMemberships.removeUserFromChannel(user, channel)
        assertEquals(0, repoMemberships.findAll().size)
    }

    @Test
    fun `find user in channel`() {
        val channel = repoChannels.findAll().first()
        val user = repoUsers.findByUsername("AntonioBanderas")!!
        repoMemberships.addUserToChannel(user, channel, AccessType.READ_WRITE)
        assertEquals(1, repoMemberships.findAll().size)
        val member = repoMemberships.findUserInChannel(user, channel)
        assertNotNull(member)
        assertEquals(user, member!!.user)
        assertEquals(channel, member.channel)
        assertEquals(AccessType.READ_WRITE, member.accessType)
    }

    @Test
    fun `find all channels for user`() {
        val user = repoUsers.findByUsername("AntonioBanderas")!!
        val channel1 = repoChannels.findAll().first()
        val channel2 = repoChannels.findAll().last()
        repoMemberships.addUserToChannel(user, channel1, AccessType.READ_WRITE)
        repoMemberships.addUserToChannel(user, channel2, AccessType.READ_WRITE)
        assertEquals(2, repoMemberships.findAllChannelsForUser(user, 2, 0).size)
    }

    @Test
    fun `find all members`() {
        val channel = repoChannels.findAll().first()
        val user = repoUsers.findByUsername("AntonioBanderas")!!
        repoMemberships.addUserToChannel(user, channel, AccessType.READ_WRITE)
        assertEquals(1, repoMemberships.findAll().size)
    }

    @Test
    fun `find member by id`() {
        val channel = repoChannels.findAll().first()
        val user = repoUsers.findByUsername("AntonioBanderas")!!
        val member = repoMemberships.addUserToChannel(user, channel, AccessType.READ_WRITE)
        val foundMember = repoMemberships.findById(member.id)
        assertNotNull(foundMember)
        assertEquals(member, foundMember)
    }

    @Test
    fun `save member`() {
        val channel = repoChannels.findAll().first()
        val user = repoUsers.findByUsername("AntonioBanderas")!!
        val member = repoMemberships.addUserToChannel(user, channel, AccessType.READ_WRITE)
        val updatedMember = member.copy(accessType = AccessType.READ_ONLY)
        repoMemberships.save(updatedMember)
        val foundMember = repoMemberships.findById(member.id)
        assertNotNull(foundMember)
        assertEquals(AccessType.READ_ONLY, foundMember!!.accessType)
    }

    @Test
    fun `delete member by id`() {
        val channel = repoChannels.findAll().first()
        val user = repoUsers.findByUsername("AntonioBanderas")!!
        val member = repoMemberships.addUserToChannel(user, channel, AccessType.READ_WRITE)
        repoMemberships.deleteById(member.id)
        assertNull(repoMemberships.findById(member.id))
    }

    @Test
    fun `clear all members`() {
        val channel = repoChannels.findAll().first()
        val user = repoUsers.findByUsername("AntonioBanderas")!!
        repoMemberships.addUserToChannel(user, channel, AccessType.READ_WRITE)
        repoMemberships.clear()
        assertEquals(0, repoMemberships.findAll().size)
    }
}
