package pt.isel.repositories.mem;

import pt.isel.repositories.Transaction;
import pt.isel.repositories.TransactionManager;
import pt.isel.repositories.channels.ChannelMemberRepository;
import pt.isel.repositories.channels.ChannelRepository;
import pt.isel.repositories.invitations.InvitationRepository;
import pt.isel.repositories.messages.MessageRepository;
import pt.isel.repositories.security.TokenBlacklistRepository;
import pt.isel.repositories.users.UserRepository;

import java.util.function.Function;

public class TransactionManagerInMem implements TransactionManager {
    private final UserRepository repoUsers = new UserRepositoryInMem();
    private final ChannelRepository repoChannels = new ChannelRepositoryInMem();
    private final MessageRepository repoMessages = new MessageRepositoryInMem();
    private final ChannelMemberRepository repoMemberships = new ChannelMemberRepositoryInMem();
    private final InvitationRepository repoInvitations = new InvitationRepositoryInMem();
    private final TokenBlacklistRepository repoTokenBlacklist = new TokenBlacklistRepositoryInMem();

    @Override
    public <R> R run(Function<Transaction, R> block) {
        return block.apply(new Transaction() {
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
            public void rollback() { /* No-op in memory */ }
        });
    }
}