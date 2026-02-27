package pt.isel.repositories.jdbi.transaction

import org.jdbi.v3.core.Handle
import pt.isel.repositories.Transaction
import pt.isel.repositories.jdbi.channels.ChannelMemberRepositoryJdbi
import pt.isel.repositories.jdbi.channels.ChannelRepositoryJdbi
import pt.isel.repositories.jdbi.invitations.InvitationRepositoryJdbi
import pt.isel.repositories.jdbi.messages.MessageRepositoryJdbi
import pt.isel.repositories.jdbi.security.TokenBlacklistRepositoryJdbi
import pt.isel.repositories.jdbi.users.UserRepositoryJdbi

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
