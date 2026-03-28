package pt.isel.domain.builders;

import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.invitations.Invitation;
import pt.isel.domain.invitations.InvitationStatus;
import pt.isel.domain.users.UserInfo;

import java.time.LocalDateTime;

public class InvitationBuilder {
    private Long id = 10000L;
    private String token = "token123";
    private UserInfo createdBy = new UserInfoBuilder().build();
    private Channel channel = new ChannelBuilder().build();
    private AccessType accessType = AccessType.READ_ONLY;
    private LocalDateTime expiresAt = LocalDateTime.of(2030, 1, 1, 12, 0);
    private InvitationStatus status = InvitationStatus.PENDING;

    public InvitationBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public InvitationBuilder withToken(String token) {
        this.token = token;
        return this;
    }

    public InvitationBuilder withCreatedBy(UserInfo createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public InvitationBuilder withChannel(Channel channel) {
        this.channel = channel;
        return this;
    }

    public InvitationBuilder withAccessType(AccessType accessType) {
        this.accessType = accessType;
        return this;
    }

    public InvitationBuilder withExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public InvitationBuilder withStatus(InvitationStatus status) {
        this.status = status;
        return this;
    }

    public Invitation build() {
        return new Invitation(id, token, createdBy, channel, accessType, expiresAt, status);
    }
}