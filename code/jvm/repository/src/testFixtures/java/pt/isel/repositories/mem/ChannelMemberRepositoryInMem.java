package pt.isel.repositories.mem;

import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.channels.ChannelMember;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.channels.ChannelMemberRepository;

import java.util.ArrayList;
import java.util.List;

public class ChannelMemberRepositoryInMem implements ChannelMemberRepository {
    private final List<ChannelMember> members = new ArrayList<>();
    private long nextId = 1;

    @Override
    public ChannelMember addUserToChannel(UserInfo userInfo, Channel channel, AccessType accessType) {
        if (findUserInChannel(userInfo.id(), channel.id()) != null)
            throw new RuntimeException("channel_members_user_id_channel_id_key");
        ChannelMember member = new ChannelMember(nextId++, userInfo, channel, accessType);
        members.add(member);
        return member;
    }

    @Override
    public ChannelMember findUserInChannel(Long userId, Long channelId) {
        return members.stream()
                .filter(m -> m.user().id().equals(userId) && m.channel().id().equals(channelId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<ChannelMember> findAllChannelsForUser(Long userId, int limit, int offset) {
        return members.stream()
                .filter(m -> m.user().id().equals(userId))
                .skip(offset)
                .limit(limit)
                .toList();
    }

    @Override
    public List<ChannelMember> findAllMembersInChannel(Long channelId, int limit, int offset) {
        return members.stream()
                .filter(m -> m.channel().id().equals(channelId))
                .skip(offset)
                .limit(limit)
                .toList();
    }

    @Override
    public void removeUserFromChannel(Long userId, Long channelId) {
        members.removeIf(m -> m.user().id().equals(userId) && m.channel().id().equals(channelId));
    }

    @Override
    public ChannelMember findById(Long id) {
        return members.stream().filter(m -> m.id().equals(id)).findFirst().orElse(null);
    }

    @Override
    public List<ChannelMember> findAll() {
        return new ArrayList<>(members);
    }

    @Override
    public void save(ChannelMember entity) {
        members.removeIf(m -> m.id().equals(entity.id()));
        members.add(entity);
    }

    @Override
    public void deleteById(Long id) {
        members.removeIf(m -> m.id().equals(id));
    }

    @Override
    public void clear() {
        members.clear();
        nextId = 1;
    }
}