package pt.isel.repositories

import pt.isel.domain.auth.PasswordValidationInfo
import pt.isel.mem.ChannelRepositoryInMem
import pt.isel.mem.UserRepositoryInMem
import kotlin.test.Test
import kotlin.test.assertEquals

class ChannelRepositoryTest {
    private val repChannels = ChannelRepositoryInMem()

    private val repUsers =
        UserRepositoryInMem().also {
            it.create("AntonioBanderas", PasswordValidationInfo(PASSWORD))
            it.create("AngelinaJolie", PasswordValidationInfo(PASSWORD))
        }

    @Test
    fun `create a public channel`() {
        val user = repUsers.findByUsername("AntonioBanderas")!!
        val channel = repChannels.create("Channel1", user, true)
        assertEquals(1, channel.id)
        assertEquals("Channel1", channel.name)
        assertEquals(user, channel.owner)
        assertEquals(true, channel.isPublic)
    }

    @Test
    fun `create a private channel`() {
        val user = repUsers.findByUsername("AntonioBanderas")!!
        val channel = repChannels.create("Channel2", user, false)
        assertEquals(1, channel.id)
        assertEquals("Channel2", channel.name)
        assertEquals(user, channel.owner)
        assertEquals(false, channel.isPublic)
    }

    @Test
    fun `create more than one channel`() {
        val user = repUsers.findByUsername("AntonioBanderas")!!
        val channel1 = repChannels.create("Channel1", user, true)
        val channel2 = repChannels.create("Channel2", user, false)
        // Check channel1
        assertEquals(1, channel1.id)
        assertEquals("Channel1", channel1.name)
        assertEquals(user, channel1.owner)
        assertEquals(true, channel1.isPublic)
        // Check channel2
        assertEquals(2, channel2.id)
        assertEquals("Channel2", channel2.name)
        assertEquals(user, channel2.owner)
        assertEquals(false, channel2.isPublic)
    }

    @Test
    fun `find channel by id`() {
        val user = repUsers.findByUsername("AntonioBanderas")!!
        val channel = repChannels.create("Channel1", user, true)
        assertEquals(channel, repChannels.findById(1))
    }

    @Test
    fun `find channel by name`() {
        val user = repUsers.findByUsername("AntonioBanderas")!!
        val channel = repChannels.create("Channel1", user, true)
        assertEquals(channel, repChannels.findByName("Channel1"))
    }

    @Test
    fun `find all public channels`() {
        val user = repUsers.findByUsername("AntonioBanderas")!!
        val channel1 = repChannels.create("Channel1", user, true)
        repChannels.create("Channel2", user, false)
        val channel3 = repChannels.create("Channel3", user, true)
        assertEquals(listOf(channel1, channel3), repChannels.findAllPublicChannels(2, 0))
    }

    @Test
    fun `find all channels by owner`() {
        val user1 = repUsers.findByUsername("AntonioBanderas")!!
        val user2 = repUsers.findByUsername("AngelinaJolie")!!
        val channel1 = repChannels.create("Channel1", user1, true)
        repChannels.create("Channel2", user2, false)
        val channel3 = repChannels.create("Channel3", user1, true)
        assertEquals(listOf(channel1, channel3), repChannels.findAllByOwner(user1))
    }

    @Test
    fun `find all`() {
        val user1 = repUsers.findByUsername("AntonioBanderas")!!
        val user2 = repUsers.findByUsername("AngelinaJolie")!!
        val channel1 = repChannels.create("Channel1", user1, true)
        val channel2 = repChannels.create("Channel2", user2, false)
        assertEquals(listOf(channel1, channel2), repChannels.findAll())
    }

    @Test
    fun `save a channel`() {
        val user = repUsers.findByUsername("AntonioBanderas")!!
        val channel = repChannels.create("Channel1", user, true)
        val channel2 = channel.copy(name = "Channel2")
        repChannels.save(channel2)
        assertEquals(channel2, repChannels.findById(1))
    }

    @Test
    fun `delete a channel`() {
        val user = repUsers.findByUsername("AntonioBanderas")!!
        repChannels.create("Channel1", user, true)
        repChannels.deleteById(1)
        assertEquals(null, repChannels.findById(1))
    }

    @Test
    fun `clear all channels`() {
        val user = repUsers.findByUsername("AntonioBanderas")!!
        repChannels.create("Channel1", user, true)
        repChannels.create("Channel2", user, false)
        repChannels.clear()
        assertEquals(emptyList(), repChannels.findAll())
    }
}
