package pt.isel.domain.builders;

import pt.isel.domain.channels.Channel;
import pt.isel.domain.users.UserInfo;

import java.util.concurrent.atomic.AtomicLong;

public class ChannelBuilder {
    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);

    private Long id = ID_GENERATOR.getAndIncrement();
    private String name = "Channel_" + id;
    private UserInfo owner = new UserInfoBuilder().build();
    private boolean isPublic = true;

    public ChannelBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public ChannelBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ChannelBuilder withOwner(UserInfo owner) {
        this.owner = owner;
        return this;
    }

    public ChannelBuilder withIsPublic(boolean isPublic) {
        this.isPublic = isPublic;
        return this;
    }

    public Channel build() {
        return new Channel(id, name, owner, isPublic);
    }
}