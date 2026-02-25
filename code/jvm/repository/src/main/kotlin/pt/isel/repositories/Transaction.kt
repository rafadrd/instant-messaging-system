package pt.isel.repositories

/**
 * The lifecycle of a pt.isel.repositories.Transaction is managed outside the scope of the IoC/DI container. Transactions
 * are instantiated by a pt.isel.repositories.TransactionManager, which is managed by the IoC/DI container (e.g.,
 * Spring). The implementation of pt.isel.repositories.Transaction is responsible for creating the necessary repository
 * instances in its constructor.
 */
interface Transaction {
    val repoUsers: UserRepository
    val repoChannels: ChannelRepository
    val repoMessages: MessageRepository
    val repoMemberships: ChannelMemberRepository
    val repoInvitations: InvitationRepository
    val repoTokenBlacklist: TokenBlacklistRepository

    fun rollback()
}
