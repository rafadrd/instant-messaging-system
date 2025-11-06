package pt.isel

import org.jdbi.v3.core.Handle
import java.sql.ResultSet
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class MessageRepositoryJdbi(
    private val handle: Handle,
) : MessageRepository {
    override fun create(
        content: String,
        user: UserInfo,
        channel: Channel,
    ): Message {
        val createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)
        val id =
            handle.executeUpdateAndReturnId(
                """
                INSERT INTO dbo.messages (content, user_id, channel_id, created_at)
                VALUES (:content, :user_id, :channel_id, :created_at)
                """,
                mapOf(
                    "content" to content,
                    "user_id" to user.id,
                    "channel_id" to channel.id,
                    "created_at" to createdAt,
                ),
            )
        return Message(id, content, user, channel, createdAt)
    }

    private val baseQuery = """
        SELECT
            m.id as msg_id, m.content, m.created_at,
            author.id AS author_id, author.username AS author_username,
            c.id AS channel_id, c.name AS channel_name, c.is_public AS channel_is_public,
            owner.id AS owner_id, owner.username AS owner_username
        FROM dbo.messages m
        JOIN dbo.users author ON m.user_id = author.id
        JOIN dbo.channels c ON m.channel_id = c.id
        JOIN dbo.users owner ON c.owner_id = owner.id
    """

    override fun findById(id: Long): Message? =
        handle.executeQueryToSingle(
            "$baseQuery WHERE m.id = :id",
            mapOf("id" to id),
            ::mapRowToMessage,
        )

    override fun findAllInChannel(
        channel: Channel,
        limit: Int,
        offset: Int,
    ): List<Message> =
        handle.executeQueryToList(
            """
            $baseQuery
            WHERE m.channel_id = :channel_id
            ORDER BY m.created_at ASC
            OFFSET :offset
            LIMIT :limit
            """,
            mapOf("channel_id" to channel.id, "offset" to offset, "limit" to limit),
            ::mapRowToMessage,
        )

    override fun findAll(): List<Message> =
        handle.executeQueryToList(
            baseQuery,
            mapper = ::mapRowToMessage,
        )

    override fun save(entity: Message) {
        handle.executeUpdate(
            """
            UPDATE dbo.messages
            SET content = :content
            WHERE id = :id
            """,
            mapOf("id" to entity.id, "content" to entity.content),
        )
    }

    override fun deleteById(id: Long) {
        handle.executeUpdate("DELETE FROM dbo.messages WHERE id = :id", mapOf("id" to id))
    }

    override fun clear() {
        handle.executeUpdate("DELETE FROM dbo.messages")
    }

    private fun mapRowToMessage(rs: ResultSet): Message {
        val author =
            UserInfo(
                rs.getLong("author_id"),
                rs.getString("author_username"),
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

        return Message(
            rs.getLong("msg_id"),
            rs.getString("content"),
            author,
            channel,
            rs.getTimestamp("created_at").toLocalDateTime().truncatedTo(ChronoUnit.MILLIS),
        )
    }
}
