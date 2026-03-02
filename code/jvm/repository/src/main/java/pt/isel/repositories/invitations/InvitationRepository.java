package pt.isel.repositories.invitations;

import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.invitations.Invitation;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.Repository;

import java.time.LocalDateTime;
import java.util.List;

public interface InvitationRepository extends Repository<Invitation> {
    Invitation create(String token, UserInfo createdBy, Channel channel, AccessType accessType, LocalDateTime expiresAt);

    Invitation findByToken(String token);

    List<Invitation> findByChannelId(Long channelId);
}