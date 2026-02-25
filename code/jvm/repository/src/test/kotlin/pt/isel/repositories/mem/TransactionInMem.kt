package pt.isel.repositories.mem

import pt.isel.repositories.ChannelMemberRepository
import pt.isel.repositories.ChannelRepository
import pt.isel.repositories.InvitationRepository
import pt.isel.repositories.MessageRepository
import pt.isel.repositories.TokenBlacklistRepository
import pt.isel.repositories.Transaction
import pt.isel.repositories.UserRepository

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
