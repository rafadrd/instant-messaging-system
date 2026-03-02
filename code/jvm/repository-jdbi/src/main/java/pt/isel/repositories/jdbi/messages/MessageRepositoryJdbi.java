package pt.isel.repositories.jdbi.messages;

import org.jdbi.v3.core.Handle;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.messages.Message;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.jdbi.utils.JdbiUtils;
import pt.isel.repositories.messages.MessageRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class MessageRepositoryJdbi implements MessageRepository {
    private final Handle handle;
    private final String baseQuery = """
            SELECT
                m.id as msg_id, m.content, m.created_at,
                author.id AS author_id, author.username AS author_username,
                c.id AS channel_id, c.name AS channel_name, c.is_public AS channel_is_public,
                owner.id AS owner_id, owner.username AS owner_username
            FROM messages m
            LEFT JOIN users author ON m.user_id = author.id
            JOIN channels c ON m.channel_id = c.id
            JOIN users owner ON c.owner_id = owner.id
            """;

    public MessageRepositoryJdbi(Handle handle) {
        this.handle = handle;
    }

    @Override
    public Message create(String content, UserInfo user, Channel channel) {
        LocalDateTime createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        String sql = """
                INSERT INTO messages (content, user_id, channel_id, created_at)
                VALUES (:content, :user_id, :channel_id, :created_at)
                """;
        Long id = JdbiUtils.executeUpdateAndReturnId(handle, sql, JdbiUtils.params(
                "content", content,
                "user_id", user.id(),
                "channel_id", channel.id(),
                "created_at", createdAt
        ));
        return new Message(id, content, user, channel, createdAt);
    }

    @Override
    public Message findById(Long id) {
        return JdbiUtils.executeQueryToSingle(handle, baseQuery + " WHERE m.id = :id",
                Map.of("id", id), this::mapRowToMessage);
    }

    @Override
    public List<Message> findAllInChannel(Channel channel, int limit, int offset) {
        return JdbiUtils.executeQueryToList(handle, """
                        %s
                        WHERE m.channel_id = :channel_id
                        ORDER BY m.created_at DESC
                        OFFSET :offset
                        LIMIT :limit
                        """.formatted(baseQuery), JdbiUtils.params("channel_id", channel.id(), "offset", offset, "limit", limit),
                this::mapRowToMessage);
    }

    @Override
    public List<Message> findAll() {
        return JdbiUtils.executeQueryToList(handle, baseQuery, Map.of(), this::mapRowToMessage);
    }

    @Override
    public void save(Message entity) {
        JdbiUtils.executeUpdate(handle, """
                UPDATE messages
                SET content = :content
                WHERE id = :id
                """, JdbiUtils.params("id", entity.id(), "content", entity.content()));
    }

    @Override
    public void deleteById(Long id) {
        JdbiUtils.executeUpdate(handle, "DELETE FROM messages WHERE id = :id", Map.of("id", id));
    }

    @Override
    public void clear() {
        JdbiUtils.executeUpdate(handle, "DELETE FROM messages", Map.of());
    }

    private Message mapRowToMessage(ResultSet rs) throws SQLException {
        long authorId = rs.getLong("author_id");
        UserInfo author = rs.wasNull() ? null : new UserInfo(authorId, rs.getString("author_username"));

        UserInfo owner = new UserInfo(rs.getLong("owner_id"), rs.getString("owner_username"));
        Channel channel = new Channel(rs.getLong("channel_id"), rs.getString("channel_name"), owner, rs.getBoolean("channel_is_public"));

        return new Message(
                rs.getLong("msg_id"),
                rs.getString("content"),
                author,
                channel,
                rs.getTimestamp("created_at").toLocalDateTime().truncatedTo(ChronoUnit.MILLIS)
        );
    }
}