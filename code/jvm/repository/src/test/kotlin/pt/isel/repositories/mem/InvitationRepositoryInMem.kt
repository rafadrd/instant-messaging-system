package pt.isel.repositories.mem

import jakarta.inject.Named
import pt.isel.domain.channels.AccessType
import pt.isel.domain.channels.Channel
import pt.isel.domain.invitations.Invitation
import pt.isel.domain.users.UserInfo
import pt.isel.repositories.invitations.InvitationRepository
import java.time.LocalDateTime

/**
 * Naif in memory repository non thread-safe and basic sequential id. Useful for unit tests purpose.
 */
@Named
class InvitationRepositoryInMem : InvitationRepository {
    private val invitations = mutableListOf<Invitation>()

    override fun create(
        token: String,
        createdBy: UserInfo,
        channel: Channel,
        accessType: AccessType,
        expiresAt: LocalDateTime,
    ): Invitation =
        Invitation(
            invitations.size.toLong() + 1,
            token,
            createdBy,
            channel,
            accessType,
            expiresAt,
        ).also { invitations.add(it) }

    override fun findById(id: Long): Invitation? = invitations.firstOrNull { it.id == id }

    override fun findByToken(token: String): Invitation? = invitations.firstOrNull { it.token == token }

    override fun findByChannelId(channelId: Long): List<Invitation> = invitations.filter { it.channel.id == channelId }

    override fun findAll(): List<Invitation> = invitations.toList()

    override fun save(entity: Invitation) {
        invitations.removeIf { it.id == entity.id }.apply { invitations.add(entity) }
    }

    override fun deleteById(id: Long) {
        invitations.removeIf { it.id == id }
    }

    override fun clear() {
        invitations.clear()
    }
}
