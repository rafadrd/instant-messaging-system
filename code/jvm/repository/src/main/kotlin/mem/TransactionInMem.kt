package pt.isel.mem

import pt.isel.ChannelMemberRepository
import pt.isel.ChannelRepository
import pt.isel.InvitationRepository
import pt.isel.MessageRepository
import pt.isel.TokenBlacklistRepository
import pt.isel.Transaction
import pt.isel.UserRepository

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
