package pt.isel.repositories.mem;

import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.invitations.Invitation;
import pt.isel.domain.invitations.InvitationStatus;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.invitations.InvitationRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InvitationRepositoryInMem implements InvitationRepository {
    private final List<Invitation> invitations = new ArrayList<>();
    private long nextId = 1;

    @Override
    public Invitation create(String token, UserInfo createdBy, Channel channel, AccessType accessType, LocalDateTime expiresAt) {
        if (findByToken(token) != null) throw new RuntimeException("invitations_token_key");
        Invitation inv = new Invitation(nextId++, token, createdBy, channel, accessType, expiresAt);
        invitations.add(inv);
        return inv;
    }

    @Override
    public Invitation findByToken(String token) {
        return invitations.stream().filter(i -> i.token().equals(token)).findFirst().orElse(null);
    }

    @Override
    public List<Invitation> findByChannelId(Long channelId) {
        return invitations.stream().filter(i -> i.channel().id().equals(channelId)).toList();
    }

    @Override
    public boolean consume(Long id) {
        for (int i = 0; i < invitations.size(); i++) {
            Invitation inv = invitations.get(i);
            if (inv.id().equals(id) && inv.status() == InvitationStatus.PENDING) {
                invitations.set(i, new Invitation(inv.id(), inv.token(), inv.createdBy(), inv.channel(), inv.accessType(), inv.expiresAt(), InvitationStatus.ACCEPTED));
                return true;
            }
        }
        return false;
    }

    @Override
    public Invitation findById(Long id) {
        return invitations.stream().filter(i -> i.id().equals(id)).findFirst().orElse(null);
    }

    @Override
    public List<Invitation> findAll() {
        return new ArrayList<>(invitations);
    }

    @Override
    public void save(Invitation entity) {
        invitations.removeIf(i -> i.id().equals(entity.id()));
        invitations.add(entity);
    }

    @Override
    public void deleteById(Long id) {
        invitations.removeIf(i -> i.id().equals(id));
    }

    @Override
    public void clear() {
        invitations.clear();
        nextId = 1;
    }
}