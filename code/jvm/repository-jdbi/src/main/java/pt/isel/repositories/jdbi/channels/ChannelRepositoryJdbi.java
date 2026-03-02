package pt.isel.repositories.jdbi.channels;

import org.jdbi.v3.core.Handle;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.channels.ChannelRepository;
import pt.isel.repositories.jdbi.utils.JdbiUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ChannelRepositoryJdbi implements ChannelRepository {
    private final Handle handle;

    public ChannelRepositoryJdbi(Handle handle) {
        this.handle = handle;
    }

    @Override
    public Channel create(String name, UserInfo owner, boolean isPublic) {
        String sql = """
                INSERT INTO dbo.channels (name, owner_id, is_public)
                VALUES (:name, :owner_id, :is_public)
                """;
        Long id = JdbiUtils.executeUpdateAndReturnId(handle, sql, JdbiUtils.params(
                "name", name,
                "owner_id", owner.id(),
                "is_public", isPublic
        ));
        return new Channel(id, name, owner, isPublic);
    }

    @Override
    public Channel findById(Long id) {
        return JdbiUtils.executeQueryToSingle(handle, """
                SELECT c.id as channel_id, c.name, c.is_public, u.id as owner_id, u.username 
                FROM dbo.channels c
                JOIN dbo.users u ON c.owner_id = u.id
                WHERE c.id = :id
                """, Map.of("id", id), this::mapRowToChannel);
    }

    @Override
    public Channel findByName(String name) {
        return JdbiUtils.executeQueryToSingle(handle, """
                SELECT c.id as channel_id, c.name, c.is_public, u.id as owner_id, u.username 
                FROM dbo.channels c
                JOIN dbo.users u ON c.owner_id = u.id
                WHERE c.name = :name
                """, Map.of("name", name), this::mapRowToChannel);
    }

    @Override
    public List<Channel> findAllByOwner(Long ownerId) {
        return JdbiUtils.executeQueryToList(handle, """
                SELECT c.id as channel_id, c.name, c.is_public, u.id as owner_id, u.username 
                FROM dbo.channels c
                JOIN dbo.users u ON c.owner_id = u.id
                WHERE owner_id = :owner_id
                """, Map.of("owner_id", ownerId), this::mapRowToChannel);
    }

    @Override
    public List<Channel> findAllPublicChannels(int limit, int offset) {
        return JdbiUtils.executeQueryToList(handle, """
                SELECT c.id as channel_id, c.name, c.is_public, u.id as owner_id, u.username 
                FROM dbo.channels c
                JOIN dbo.users u ON c.owner_id = u.id
                WHERE c.is_public = TRUE
                ORDER BY c.id ASC
                OFFSET :offset
                LIMIT :limit
                """, JdbiUtils.params("offset", offset, "limit", limit), this::mapRowToChannel);
    }

    @Override
    public List<Channel> findAll() {
        return JdbiUtils.executeQueryToList(handle, """
                SELECT c.id as channel_id, c.name, c.is_public, u.id as owner_id, u.username 
                FROM dbo.channels c
                JOIN dbo.users u ON c.owner_id = u.id
                """, Map.of(), this::mapRowToChannel);
    }

    @Override
    public List<Channel> searchByName(String query, int limit, int offset) {
        String escapedQuery = query.replace("%", "\\%").replace("_", "\\_");
        return JdbiUtils.executeQueryToList(handle, """
                SELECT c.id as channel_id, c.name, c.is_public, u.id as owner_id, u.username 
                FROM dbo.channels c
                JOIN dbo.users u ON c.owner_id = u.id
                WHERE c.name ILIKE '%' || :query || '%' ESCAPE '\\' AND c.is_public = TRUE
                ORDER BY c.id ASC
                OFFSET :offset
                LIMIT :limit
                """, JdbiUtils.params("query", escapedQuery, "offset", offset, "limit", limit), this::mapRowToChannel);
    }

    @Override
    public void save(Channel entity) {
        JdbiUtils.executeUpdate(handle, """
                UPDATE dbo.channels
                SET name = :name, is_public = :is_public
                WHERE id = :id
                """, JdbiUtils.params(
                "id", entity.id(),
                "name", entity.name(),
                "is_public", entity.isPublic()
        ));
    }

    @Override
    public void deleteById(Long id) {
        JdbiUtils.executeUpdate(handle, "DELETE FROM dbo.channels WHERE id = :id", Map.of("id", id));
    }

    @Override
    public void clear() {
        JdbiUtils.executeUpdate(handle, "DELETE FROM dbo.channels", Map.of());
    }

    private Channel mapRowToChannel(ResultSet rs) throws SQLException {
        UserInfo owner = new UserInfo(rs.getLong("owner_id"), rs.getString("username"));
        return new Channel(
                rs.getLong("channel_id"),
                rs.getString("name"),
                owner,
                rs.getBoolean("is_public")
        );
    }
}