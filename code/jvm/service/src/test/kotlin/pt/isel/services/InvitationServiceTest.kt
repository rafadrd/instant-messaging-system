package pt.isel.services

import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pt.isel.auth.Sha256TokenEncoder
import pt.isel.auth.UsersDomainConfig
import pt.isel.domain.channel.AccessType
import pt.isel.domain.invitation.Invitation
import pt.isel.domain.security.PasswordSecurityDomain
import pt.isel.mem.TransactionManagerInMem
import pt.isel.repositories.jdbi.TransactionManagerJdbi
import pt.isel.repositories.jdbi.configureWithAppRequirements
import java.time.LocalDateTime
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class InvitationServiceTest {
    companion object {
        private val jdbi =
            Jdbi
                .create(PGSimpleDataSource().apply { setURL(Environment.getDbUrl()) })
                .configureWithAppRequirements()

        @JvmStatic
        fun transactionManagers(): Stream<TransactionManager> =
            Stream.of(
                TransactionManagerInMem().also { cleanup(it) },
                TransactionManagerJdbi(jdbi).also { cleanup(it) },
            )

        private fun cleanup(trxManager: TransactionManager) {
            trxManager.run {
                repoInvitations.clear()
                repoMemberships.clear()
                repoMessages.clear()
                repoChannels.clear()
                repoUsers.clear()
            }
        }

        private fun createUserService(
            trxManager: TransactionManager,
            testClock: TestClock,
            tokenTtl: Duration = 30.days,
            tokenRollingTtl: Duration = 30.minutes,
            maxTokensPerUser: Int = 3,
        ) = UserService(
            trxManager,
            PasswordSecurityDomain(
                BCryptPasswordEncoder(),
                Sha256TokenEncoder(),
                UsersDomainConfig(
                    tokenSizeInBytes = 256 / 8,
                    tokenTtl = tokenTtl,
                    tokenRollingTtl,
                    maxTokensPerUser = maxTokensPerUser,
                ),
            ),
            testClock,
        )
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `create invitation`(trxManager: TransactionManager) {
        val testClock = TestClock()
        val userService = createUserService(trxManager, testClock)
        val channelService = ChannelService(trxManager)
        val invitationService = InvitationService(trxManager)
        val creator = userService.registerUser("creator", VALID_PASSWORD)
        assertTrue(creator is Either.Right)
        val channel = channelService.createChannel("channel", creator.value.id, isPublic = false)
        assertTrue(channel is Either.Right)
        val invitation =
            invitationService.createInvitation(
                creator.value.id,
                channel.value.id,
                AccessType.READ_WRITE,
                LocalDateTime.now().plusDays(7),
            )
        assertTrue(invitation is Either.Right)
        assertEquals(
            Invitation(
                invitation.value.id,
                invitation.value.token,
                creator.value,
                channel.value,
                AccessType.READ_WRITE,
                invitation.value.expiresAt,
            ),
            invitation.value,
        )
    }
}
