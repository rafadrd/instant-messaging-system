package pt.isel.repositories.jdbi.invitations;

import org.jdbi.v3.core.Handle;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.invitations.Invitation;
import pt.isel.domain.invitations.InvitationStatus;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.invitations.InvitationRepository;
import pt.isel.repositories.jdbi.utils.JdbiUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class InvitationRepositoryJdbi implements InvitationRepository {
    private final Handle handle;
    private final String baseQuery = """
            SELECT
                i.id as inv_id, i.token, i.access_type, i.expires_at, i.status,
                creator.id AS creator_id, creator.username AS creator_username,
                c.id AS channel_id, c.name AS channel_name, c.is_public AS channel_is_public,
                owner.id AS owner_id, owner.username AS owner_username
            FROM dbo.invitations i
            JOIN dbo.users creator ON i.created_by = creator.id
            JOIN dbo.channels c ON i.channel_id = c.id
            JOIN dbo.users owner ON c.owner_id = owner.id
            """;

    public InvitationRepositoryJdbi(Handle handle) {
        this.handle = handle;
    }

    @Override
    public Invitation create(String token, UserInfo createdBy, Channel channel, AccessType accessType, LocalDateTime expiresAt) {
        LocalDateTime expiration = expiresAt.truncatedTo(ChronoUnit.MILLIS);
        String sql = """
                INSERT INTO dbo.invitations (token, created_by, channel_id, access_type, expires_at) 
                VALUES (:token, :created_by, :channel_id, :access_type, :expires_at)
                """;
        Long id = JdbiUtils.executeUpdateAndReturnId(handle, sql, JdbiUtils.params(
                "token", token,
                "created_by", createdBy.id(),
                "channel_id", channel.id(),
                "access_type", accessType.name(),
                "expires_at", expiration
        ));
        return new Invitation(id, token, createdBy, channel, accessType, expiration, InvitationStatus.PENDING);
    }

    @Override
    public Invitation findById(Long id) {
        return JdbiUtils.executeQueryToSingle(handle, baseQuery + " WHERE i.id = :id",
                Map.of("id", id), this::mapRowToInvitation);
    }

    @Override
    public Invitation findByToken(String token) {
        return JdbiUtils.executeQueryToSingle(handle, baseQuery + " WHERE i.token = :token",
                Map.of("token", token), this::mapRowToInvitation);
    }

    @Override
    public List<Invitation> findByChannelId(Long channelId) {
        return JdbiUtils.executeQueryToList(handle, baseQuery + " WHERE i.channel_id = :channelId",
                Map.of("channelId", channelId), this::mapRowToInvitation);
    }

    @Override
    public List<Invitation> findAll() {
        return JdbiUtils.executeQueryToList(handle, baseQuery, Map.of(), this::mapRowToInvitation);
    }

    @Override
    public void save(Invitation entity) {
        JdbiUtils.executeUpdate(handle, """
                UPDATE dbo.invitations
                SET status = :status
                WHERE id = :id
                """, JdbiUtils.params("id", entity.id(), "status", entity.status().name()));
    }

    @Override
    public void deleteById(Long id) {
        JdbiUtils.executeUpdate(handle, "DELETE FROM dbo.invitations WHERE id = :id", Map.of("id", id));
    }

    @Override
    public void clear() {
        JdbiUtils.executeUpdate(handle, "DELETE FROM dbo.invitations", Map.of());
    }

    private Invitation mapRowToInvitation(ResultSet rs) throws SQLException {
        UserInfo creator = new UserInfo(rs.getLong("creator_id"), rs.getString("creator_username"));
        UserInfo owner = new UserInfo(rs.getLong("owner_id"), rs.getString("owner_username"));
        Channel channel = new Channel(rs.getLong("channel_id"), rs.getString("channel_name"), owner, rs.getBoolean("channel_is_public"));

        return new Invitation(
                rs.getLong("inv_id"),
                rs.getString("token"),
                creator,
                channel,
                AccessType.valueOf(rs.getString("access_type")),
                rs.getTimestamp("expires_at").toLocalDateTime().truncatedTo(ChronoUnit.MILLIS),
                InvitationStatus.valueOf(rs.getString("status"))
        );
    }
}