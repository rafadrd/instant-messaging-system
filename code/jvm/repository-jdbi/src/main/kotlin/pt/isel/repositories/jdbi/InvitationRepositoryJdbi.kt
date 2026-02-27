package pt.isel.repositories.jdbi

import org.jdbi.v3.core.Handle
import pt.isel.domain.channel.AccessType
import pt.isel.domain.channel.Channel
import pt.isel.domain.invitation.Invitation
import pt.isel.domain.invitation.InvitationStatus
import pt.isel.domain.user.UserInfo
import pt.isel.repositories.InvitationRepository
import java.sql.ResultSet
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class InvitationRepositoryJdbi(
    private val handle: Handle,
) : InvitationRepository {
    override fun create(
        token: String,
        createdBy: UserInfo,
        channel: Channel,
        accessType: AccessType,
        expiresAt: LocalDateTime,
    ): Invitation {
        val expiration = expiresAt.truncatedTo(ChronoUnit.MILLIS)
        val id =
            handle.executeUpdateAndReturnId(
                """
                INSERT INTO dbo.invitations (token, created_by, channel_id, access_type, expires_at) 
                VALUES (:token, :created_by, :channel_id, :access_type, :expires_at)
                """,
                mapOf(
                    "token" to token,
                    "created_by" to createdBy.id,
                    "channel_id" to channel.id,
                    "access_type" to accessType,
                    "expires_at" to expiration,
                ),
            )
        return Invitation(id, token, createdBy, channel, accessType, expiration)
    }

    private val baseQuery = """
        SELECT
            i.id as inv_id, i.token, i.access_type, i.expires_at, i.status,
            creator.id AS creator_id, creator.username AS creator_username,
            c.id AS channel_id, c.name AS channel_name, c.is_public AS channel_is_public,
            owner.id AS owner_id, owner.username AS owner_username
        FROM dbo.invitations i
        JOIN dbo.users creator ON i.created_by = creator.id
        JOIN dbo.channels c ON i.channel_id = c.id
        JOIN dbo.users owner ON c.owner_id = owner.id
    """

    override fun findById(id: Long): Invitation? =
        handle.executeQueryToSingle(
            "$baseQuery WHERE i.id = :id",
            mapOf("id" to id),
            ::mapRowToInvitation,
        )

    override fun findByToken(token: String): Invitation? =
        handle.executeQueryToSingle(
            "$baseQuery WHERE i.token = :token",
            mapOf("token" to token),
            ::mapRowToInvitation,
        )

    override fun findByChannelId(channelId: Long): List<Invitation> =
        handle.executeQueryToList(
            "$baseQuery WHERE i.channel_id = :channelId",
            mapOf("channelId" to channelId),
            ::mapRowToInvitation,
        )

    override fun findAll(): List<Invitation> =
        handle.executeQueryToList(
            baseQuery,
            mapper = ::mapRowToInvitation,
        )

    override fun save(entity: Invitation) {
        handle.executeUpdate(
            """
            UPDATE dbo.invitations
            SET status = :status
            WHERE id = :id
            """,
            mapOf(
                "id" to entity.id,
                "status" to entity.status,
            ),
        )
    }

    override fun deleteById(id: Long) {
        handle.executeUpdate("DELETE FROM dbo.invitations WHERE id = :id", mapOf("id" to id))
    }

    override fun clear() {
        handle.executeUpdate("DELETE FROM dbo.invitations")
    }

    private fun mapRowToInvitation(rs: ResultSet): Invitation {
        val creator =
            UserInfo(
                rs.getLong("creator_id"),
                rs.getString("creator_username"),
            )

        val owner =
            UserInfo(
                rs.getLong("owner_id"),
                rs.getString("owner_username"),
            )

        val channel =
            Channel(
                rs.getLong("channel_id"),
                rs.getString("channel_name"),
                owner,
                rs.getBoolean("channel_is_public"),
            )

        return Invitation(
            rs.getLong("inv_id"),
            rs.getString("token"),
            creator,
            channel,
            AccessType.valueOf(rs.getString("access_type")),
            rs.getTimestamp("expires_at").toLocalDateTime().truncatedTo(ChronoUnit.MILLIS),
            InvitationStatus.valueOf(rs.getString("status")),
        )
    }
}
