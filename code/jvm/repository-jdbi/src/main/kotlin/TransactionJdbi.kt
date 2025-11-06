package pt.isel

import org.jdbi.v3.core.Handle

class TransactionJdbi(
    private val handle: Handle,
) : Transaction {
    override val repoUsers = UserRepositoryJdbi(handle)
    override val repoChannels = ChannelRepositoryJdbi(handle)
    override val repoMessages = MessageRepositoryJdbi(handle)
    override val repoMemberships = ChannelMemberRepositoryJdbi(handle)
    override val repoInvitations = InvitationRepositoryJdbi(handle)
    override val repoTokenBlacklist = TokenBlacklistRepositoryJdbi(handle)

    override fun rollback() {
        handle.rollback()
    }
}
