package pt.isel.repositories.jdbi.channels;

import org.jdbi.v3.core.Handle;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.channels.ChannelMember;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.channels.ChannelMemberRepository;
import pt.isel.repositories.jdbi.utils.JdbiUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ChannelMemberRepositoryJdbi implements ChannelMemberRepository {
    private final Handle handle;
    private final String baseQuery = """
            SELECT
                cm.id as cm_id, cm.access_type,
                member.id AS member_id, member.username AS member_username,
                c.id AS channel_id, c.name AS channel_name, c.is_public AS channel_is_public,
                owner.id AS owner_id, owner.username AS owner_username
            FROM channel_members cm
            JOIN users member ON cm.user_id = member.id
            JOIN channels c ON cm.channel_id = c.id
            JOIN users owner ON c.owner_id = owner.id
            """;

    public ChannelMemberRepositoryJdbi(Handle handle) {
        this.handle = handle;
    }

    @Override
    public ChannelMember addUserToChannel(UserInfo userInfo, Channel channel, AccessType accessType) {
        String sql = """
                INSERT INTO channel_members (user_id, channel_id, access_type)
                VALUES (:user_id, :channel_id, :access_type)
                """;
        Long id = JdbiUtils.executeUpdateAndReturnId(handle, sql, JdbiUtils.params(
                "user_id", userInfo.id(),
                "channel_id", channel.id(),
                "access_type", accessType.name()
        ));
        return new ChannelMember(id, userInfo, channel, accessType);
    }

    @Override
    public ChannelMember findById(Long id) {
        return JdbiUtils.executeQueryToSingle(handle, baseQuery + " WHERE cm.id = :id",
                JdbiUtils.params("id", id), this::mapRowToChannelMember);
    }

    @Override
    public ChannelMember findUserInChannel(Long userId, Long channelId) {
        return JdbiUtils.executeQueryToSingle(handle,
                baseQuery + " WHERE cm.channel_id = :channel_id AND cm.user_id = :user_id",
                JdbiUtils.params("channel_id", channelId, "user_id", userId),
                this::mapRowToChannelMember);
    }

    @Override
    public List<ChannelMember> findAllChannelsForUser(Long userId, int limit, int offset) {
        return JdbiUtils.executeQueryToList(handle,
                baseQuery + " WHERE cm.user_id = :user_id ORDER BY cm.id ASC OFFSET :offset LIMIT :limit",
                JdbiUtils.params("user_id", userId, "offset", offset, "limit", limit),
                this::mapRowToChannelMember);
    }

    @Override
    public List<ChannelMember> findAllMembersInChannel(Long channelId, int limit, int offset) {
        return JdbiUtils.executeQueryToList(handle,
                baseQuery + " WHERE cm.channel_id = :channel_id ORDER BY cm.id ASC OFFSET :offset LIMIT :limit",
                JdbiUtils.params("channel_id", channelId, "offset", offset, "limit", limit),
                this::mapRowToChannelMember);
    }

    @Override
    public List<ChannelMember> findAll() {
        return JdbiUtils.executeQueryToList(handle, baseQuery, Map.of(), this::mapRowToChannelMember);
    }

    @Override
    public void save(ChannelMember entity) {
        JdbiUtils.executeUpdate(handle, """
                UPDATE channel_members
                SET access_type = :accessType
                WHERE id = :id
                """, JdbiUtils.params("id", entity.id(), "accessType", entity.accessType().name()));
    }

    @Override
    public void removeUserFromChannel(Long userId, Long channelId) {
        JdbiUtils.executeUpdate(handle, """
                DELETE FROM channel_members
                WHERE user_id = :user_id AND channel_id = :channel_id
                """, JdbiUtils.params("user_id", userId, "channel_id", channelId));
    }

    @Override
    public void deleteById(Long id) {
        JdbiUtils.executeUpdate(handle, "DELETE FROM channel_members WHERE id = :id", Map.of("id", id));
    }

    @Override
    public void clear() {
        JdbiUtils.executeUpdate(handle, "DELETE FROM channel_members", Map.of());
    }

    private ChannelMember mapRowToChannelMember(ResultSet rs) throws SQLException {
        UserInfo member = new UserInfo(rs.getLong("member_id"), rs.getString("member_username"));
        UserInfo owner = new UserInfo(rs.getLong("owner_id"), rs.getString("owner_username"));
        Channel channel = new Channel(rs.getLong("channel_id"), rs.getString("channel_name"), owner, rs.getBoolean("channel_is_public"));

        return new ChannelMember(
                rs.getLong("cm_id"),
                member,
                channel,
                AccessType.valueOf(rs.getString("access_type"))
        );
    }
}