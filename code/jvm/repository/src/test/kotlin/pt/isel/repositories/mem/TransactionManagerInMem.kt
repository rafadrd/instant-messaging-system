package pt.isel.repositories.mem

import pt.isel.repositories.Transaction
import pt.isel.repositories.TransactionManager

class TransactionManagerInMem : TransactionManager {
    private val repoUsers = UserRepositoryInMem()
    private val repoChannels = ChannelRepositoryInMem()
    private val repoMessages = MessageRepositoryInMem()
    private val repoMemberships = ChannelMemberRepositoryInMem()
    private val repoInvitations = InvitationRepositoryInMem()
    private val repoTokenBlacklist = TokenBlacklistRepositoryInMem()

    override fun <R> run(block: Transaction.() -> R): R =
        block(
            TransactionInMem(
                repoUsers,
                repoChannels,
                repoMessages,
                repoMemberships,
                repoInvitations,
                repoTokenBlacklist,
            ),
        )
}
