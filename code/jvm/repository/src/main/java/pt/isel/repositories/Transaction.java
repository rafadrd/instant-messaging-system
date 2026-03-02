package pt.isel.repositories;

import pt.isel.repositories.channels.ChannelMemberRepository;
import pt.isel.repositories.channels.ChannelRepository;
import pt.isel.repositories.invitations.InvitationRepository;
import pt.isel.repositories.messages.MessageRepository;
import pt.isel.repositories.security.TokenBlacklistRepository;
import pt.isel.repositories.users.UserRepository;

public interface Transaction {
    UserRepository repoUsers();

    ChannelRepository repoChannels();

    MessageRepository repoMessages();

    ChannelMemberRepository repoMemberships();

    InvitationRepository repoInvitations();

    TokenBlacklistRepository repoTokenBlacklist();

    void rollback();
}