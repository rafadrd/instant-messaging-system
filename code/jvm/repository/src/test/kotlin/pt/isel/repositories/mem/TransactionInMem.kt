package pt.isel.repositories.mem

import pt.isel.repositories.Transaction
import pt.isel.repositories.channels.ChannelMemberRepository
import pt.isel.repositories.channels.ChannelRepository
import pt.isel.repositories.invitations.InvitationRepository
import pt.isel.repositories.messages.MessageRepository
import pt.isel.repositories.security.TokenBlacklistRepository
import pt.isel.repositories.users.UserRepository

class TransactionInMem(
    override val repoUsers: UserRepository,
    override val repoChannels: ChannelRepository,
    override val repoMessages: MessageRepository,
    override val repoMemberships: ChannelMemberRepository,
    override val repoInvitations: InvitationRepository,
    override val repoTokenBlacklist: TokenBlacklistRepository,
) : Transaction {
    override fun rollback(): Unit = throw UnsupportedOperationException()
}
