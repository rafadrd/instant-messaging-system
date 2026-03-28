package pt.isel.domain.builders;

import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.channels.ChannelMember;
import pt.isel.domain.users.UserInfo;

import java.util.concurrent.atomic.AtomicLong;

public class ChannelMemberBuilder {
    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);

    private Long id = ID_GENERATOR.getAndIncrement();
    private UserInfo user = new UserInfoBuilder().build();
    private Channel channel = new ChannelBuilder().build();
    private AccessType accessType = AccessType.READ_WRITE;

    public ChannelMemberBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public ChannelMemberBuilder withUser(UserInfo user) {
        this.user = user;
        return this;
    }

    public ChannelMemberBuilder withChannel(Channel channel) {
        this.channel = channel;
        return this;
    }

    public ChannelMemberBuilder withAccessType(AccessType accessType) {
        this.accessType = accessType;
        return this;
    }

    public ChannelMember build() {
        return new ChannelMember(id, user, channel, accessType);
    }
}