package pt.isel.repositories.jdbi.transaction;

import org.jdbi.v3.core.Handle;
import pt.isel.repositories.Transaction;
import pt.isel.repositories.channels.ChannelMemberRepository;
import pt.isel.repositories.channels.ChannelRepository;
import pt.isel.repositories.invitations.InvitationRepository;
import pt.isel.repositories.jdbi.channels.ChannelMemberRepositoryJdbi;
import pt.isel.repositories.jdbi.channels.ChannelRepositoryJdbi;
import pt.isel.repositories.jdbi.invitations.InvitationRepositoryJdbi;
import pt.isel.repositories.jdbi.messages.MessageRepositoryJdbi;
import pt.isel.repositories.jdbi.security.TokenBlacklistRepositoryJdbi;
import pt.isel.repositories.jdbi.users.UserRepositoryJdbi;
import pt.isel.repositories.messages.MessageRepository;
import pt.isel.repositories.security.TokenBlacklistRepository;
import pt.isel.repositories.users.UserRepository;

public class TransactionJdbi implements Transaction {
    private final Handle handle;

    private final UserRepository repoUsers;
    private final ChannelRepository repoChannels;
    private final MessageRepository repoMessages;
    private final ChannelMemberRepository repoMemberships;
    private final InvitationRepository repoInvitations;
    private final TokenBlacklistRepository repoTokenBlacklist;

    public TransactionJdbi(Handle handle) {
        this.handle = handle;
        this.repoUsers = new UserRepositoryJdbi(handle);
        this.repoChannels = new ChannelRepositoryJdbi(handle);
        this.repoMessages = new MessageRepositoryJdbi(handle);
        this.repoMemberships = new ChannelMemberRepositoryJdbi(handle);
        this.repoInvitations = new InvitationRepositoryJdbi(handle);
        this.repoTokenBlacklist = new TokenBlacklistRepositoryJdbi(handle);
    }

    @Override
    public UserRepository repoUsers() {
        return repoUsers;
    }

    @Override
    public ChannelRepository repoChannels() {
        return repoChannels;
    }

    @Override
    public MessageRepository repoMessages() {
        return repoMessages;
    }

    @Override
    public ChannelMemberRepository repoMemberships() {
        return repoMemberships;
    }

    @Override
    public InvitationRepository repoInvitations() {
        return repoInvitations;
    }

    @Override
    public TokenBlacklistRepository repoTokenBlacklist() {
        return repoTokenBlacklist;
    }

    @Override
    public void rollback() {
        handle.rollback();
    }
}