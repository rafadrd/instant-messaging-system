package pt.isel.repositories.jdbi

import org.jdbi.v3.core.Handle
import pt.isel.domain.channel.AccessType
import pt.isel.domain.channel.Channel
import pt.isel.domain.channel.ChannelMember
import pt.isel.domain.user.UserInfo
import pt.isel.repositories.ChannelMemberRepository
import java.sql.ResultSet

class ChannelMemberRepositoryJdbi(
    private val handle: Handle,
) : ChannelMemberRepository {
    override fun addUserToChannel(
        userInfo: UserInfo,
        channel: Channel,
        accessType: AccessType,
    ): ChannelMember {
        val id =
            handle.executeUpdateAndReturnId(
                """
                INSERT INTO dbo.channel_members (user_id, channel_id, access_type)
                VALUES (:user_id, :channel_id, :access_type)
                """,
                mapOf(
                    "user_id" to userInfo.id,
                    "channel_id" to channel.id,
                    "access_type" to accessType,
                ),
            )
        return ChannelMember(id, userInfo, channel, accessType)
    }

    private val baseQuery = """
        SELECT
            cm.id as cm_id, cm.access_type,
            member.id AS member_id, member.username AS member_username,
            c.id AS channel_id, c.name AS channel_name, c.is_public AS channel_is_public,
            owner.id AS owner_id, owner.username AS owner_username
        FROM dbo.channel_members cm
        JOIN dbo.users member ON cm.user_id = member.id
        JOIN dbo.channels c ON cm.channel_id = c.id
        JOIN dbo.users owner ON c.owner_id = owner.id
    """

    override fun findById(id: Long): ChannelMember? =
        handle.executeQueryToSingle(
            "$baseQuery WHERE cm.id = :id",
            mapOf("id" to id),
            ::mapRowToChannelMember,
        )

    override fun findUserInChannel(
        userId: Long,
        channelId: Long,
    ): ChannelMember? =
        handle.executeQueryToSingle(
            "$baseQuery WHERE cm.channel_id = :channel_id AND cm.user_id = :user_id",
            mapOf("channel_id" to channelId, "user_id" to userId),
            ::mapRowToChannelMember,
        )

    override fun findAllChannelsForUser(
        userId: Long,
        limit: Int,
        offset: Int,
    ): List<ChannelMember> =
        handle.executeQueryToList(
            "$baseQuery WHERE cm.user_id = :user_id OFFSET :offset LIMIT :limit",
            mapOf("user_id" to userId, "offset" to offset, "limit" to limit),
            ::mapRowToChannelMember,
        )

    override fun findAllMembersInChannel(
        channelId: Long,
        limit: Int,
        offset: Int,
    ): List<ChannelMember> =
        handle.executeQueryToList(
            "$baseQuery WHERE cm.channel_id = :channel_id OFFSET :offset LIMIT :limit",
            mapOf("channel_id" to channelId, "offset" to offset, "limit" to limit),
            ::mapRowToChannelMember,
        )

    override fun findAll(): List<ChannelMember> =
        handle.executeQueryToList(
            baseQuery,
            mapper = ::mapRowToChannelMember,
        )

    override fun save(entity: ChannelMember) {
        handle.executeUpdate(
            """
            UPDATE dbo.channel_members
            SET access_type = :accessType
            WHERE id = :id
            """,
            mapOf("id" to entity.id, "accessType" to entity.accessType),
        )
    }

    override fun removeUserFromChannel(
        userId: Long,
        channelId: Long,
    ) {
        handle.executeUpdate(
            """
            DELETE FROM dbo.channel_members
            WHERE user_id = :user_id AND channel_id = :channel_id
            """,
            mapOf("user_id" to userId, "channel_id" to channelId),
        )
    }

    override fun deleteById(id: Long) {
        handle.executeUpdate("DELETE FROM dbo.channel_members WHERE id = :id", mapOf("id" to id))
    }

    override fun clear() {
        handle.executeUpdate("DELETE FROM dbo.channel_members")
    }

    private fun mapRowToChannelMember(rs: ResultSet): ChannelMember {
        val member =
            UserInfo(
                rs.getLong("member_id"),
                rs.getString("member_username"),
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

        return ChannelMember(
            rs.getLong("cm_id"),
            member,
            channel,
            AccessType.valueOf(rs.getString("access_type")),
        )
    }
}
