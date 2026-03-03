package pt.isel.repositories.channels;

import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.channels.ChannelMember;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.Repository;

import java.util.List;

public interface ChannelMemberRepository extends Repository<ChannelMember> {
    ChannelMember addUserToChannel(UserInfo userInfo, Channel channel, AccessType accessType);

    ChannelMember findUserInChannel(Long userId, Long channelId);

    List<ChannelMember> findAllChannelsForUser(Long userId, int limit, int offset);

    void removeUserFromChannel(Long userId, Long channelId);

    List<ChannelMember> findAllMembersInChannel(Long channelId, int limit, int offset);
}