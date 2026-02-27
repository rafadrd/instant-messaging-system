package pt.isel.repositories.jdbi.channels

import org.jdbi.v3.core.Handle
import pt.isel.domain.channels.Channel
import pt.isel.domain.users.UserInfo
import pt.isel.repositories.channels.ChannelRepository
import pt.isel.repositories.jdbi.utils.executeQueryToList
import pt.isel.repositories.jdbi.utils.executeQueryToSingle
import pt.isel.repositories.jdbi.utils.executeUpdate
import pt.isel.repositories.jdbi.utils.executeUpdateAndReturnId
import java.sql.ResultSet

class ChannelRepositoryJdbi(
    private val handle: Handle,
) : ChannelRepository {
    override fun create(
        name: String,
        owner: UserInfo,
        isPublic: Boolean,
    ): Channel {
        val id =
            handle.executeUpdateAndReturnId(
                """
                INSERT INTO dbo.channels (name, owner_id, is_public)
                VALUES (:name, :owner_id, :is_public)
                """,
                mapOf("name" to name, "owner_id" to owner.id, "is_public" to isPublic),
            )
        return Channel(id, name, owner, isPublic)
    }

    override fun findById(id: Long): Channel? =
        handle.executeQueryToSingle(
            """
            SELECT c.id as channel_id, c.name, c.is_public, u.id as owner_id, u.username 
            FROM dbo.channels c
            JOIN dbo.users u ON c.owner_id = u.id
            WHERE c.id = :id
            """,
            mapOf("id" to id),
            ::mapRowToChannel,
        )

    override fun findByName(name: String): Channel? =
        handle.executeQueryToSingle(
            """
            SELECT c.id as channel_id, c.name, c.is_public, u.id as owner_id, u.username 
            FROM dbo.channels c
            JOIN dbo.users u ON c.owner_id = u.id
            WHERE c.name = :name
            """,
            mapOf("name" to name),
            ::mapRowToChannel,
        )

    override fun findAllByOwner(ownerId: Long): List<Channel> =
        handle.executeQueryToList(
            """
            SELECT c.id as channel_id, c.name, c.is_public, u.id as owner_id, u.username 
            FROM dbo.channels c
            JOIN dbo.users u ON c.owner_id = u.id
            WHERE owner_id = :owner_id
            """,
            mapOf("owner_id" to ownerId),
            ::mapRowToChannel,
        )

    override fun findAllPublicChannels(
        limit: Int,
        offset: Int,
    ): List<Channel> =
        handle.executeQueryToList(
            """
            SELECT c.id as channel_id, c.name, c.is_public, u.id as owner_id, u.username 
            FROM dbo.channels c
            JOIN dbo.users u ON c.owner_id = u.id
            WHERE c.is_public = TRUE
            OFFSET :offset
            LIMIT :limit
            """,
            mapOf("offset" to offset, "limit" to limit),
            ::mapRowToChannel,
        )

    override fun findAll(): List<Channel> =
        handle.executeQueryToList(
            """
            SELECT c.id as channel_id, c.name, c.is_public, u.id as owner_id, u.username 
            FROM dbo.channels c
            JOIN dbo.users u ON c.owner_id = u.id
            """,
            mapper = ::mapRowToChannel,
        )

    override fun searchByName(
        query: String,
        limit: Int,
        offset: Int,
    ): List<Channel> =
        handle.executeQueryToList(
            """
            SELECT c.id as channel_id, c.name, c.is_public, u.id as owner_id, u.username 
            FROM dbo.channels c
            JOIN dbo.users u ON c.owner_id = u.id
            WHERE c.name ILIKE '%' || :query || '%' AND c.is_public = TRUE
            OFFSET :offset
            LIMIT :limit
            """,
            mapOf("query" to query, "offset" to offset, "limit" to limit),
            ::mapRowToChannel,
        )

    override fun save(entity: Channel) {
        handle.executeUpdate(
            """
            UPDATE dbo.channels
            SET name = :name, is_public = :is_public
            WHERE id = :id
            """,
            mapOf(
                "id" to entity.id,
                "name" to entity.name,
                "is_public" to entity.isPublic,
            ),
        )
    }

    override fun deleteById(id: Long) {
        handle.executeUpdate("DELETE FROM dbo.channels WHERE id = :id", mapOf("id" to id))
    }

    override fun clear() {
        handle.executeUpdate("DELETE FROM dbo.channels")
    }

    private fun mapRowToChannel(rs: ResultSet): Channel {
        val owner =
            UserInfo(
                rs.getLong("owner_id"),
                rs.getString("username"),
            )

        return Channel(
            rs.getLong("channel_id"),
            rs.getString("name"),
            owner,
            rs.getBoolean("is_public"),
        )
    }
}
