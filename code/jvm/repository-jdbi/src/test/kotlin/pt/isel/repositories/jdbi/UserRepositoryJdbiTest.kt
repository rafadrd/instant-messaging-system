package pt.isel.repositories.jdbi

import kotlinx.datetime.Clock
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.BeforeEach
import org.postgresql.ds.PGSimpleDataSource
import pt.isel.auth.Token
import pt.isel.auth.TokenValidationInfo
import pt.isel.domain.User
import pt.isel.domain.auth.PasswordValidationInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.nanoseconds

class UserRepositoryJdbiTest {
    companion object {
        private fun runWithHandle(block: (Handle) -> Unit) = jdbi.useTransaction<Exception>(block)

        private val jdbi =
            Jdbi
                .create(PGSimpleDataSource().apply { setURL(Environment.getDbUrl()) })
                .configureWithAppRequirements()
    }

    @BeforeEach
    fun clean() {
        runWithHandle { handle: Handle ->
            MessageRepositoryJdbi(handle).clear()
            UserRepositoryJdbi(handle).clear()
        }
    }

    private fun createUser(
        username: String = "user",
        password: String = "password",
    ): User {
        val userRepo = UserRepositoryJdbi(jdbi.open())
        return userRepo.create(username, PasswordValidationInfo(password))
    }

    @Test
    fun `Test for Successful User Creation`() {
        runWithHandle {
            val user = createUser()

            assertNotNull(user)
            assertTrue(user.id > 0)
            assertEquals("user", user.username)
        }
    }

    @Test
    fun `Test for Error During Creation`() {
        runWithHandle {
            createUser()
            assertFailsWith<Exception> { createUser() }
        }
    }

    @Test
    fun `Test for Finding User by ID`() {
        runWithHandle { handle ->
            val userRepository = UserRepositoryJdbi(handle)
            val user = createUser()
            val foundUser = userRepository.findById(user.id)

            assertNotNull(foundUser)
            assertEquals(user.id, foundUser.id)
        }
    }

    @Test
    fun `Test for User Not Found`() {
        runWithHandle { handle ->
            val userRepository = UserRepositoryJdbi(handle)

            val foundUser = userRepository.findById(Long.MAX_VALUE)
            assertNull(foundUser)
        }
    }

    @Test
    fun `Test for Finding User by Username`() {
        runWithHandle { handle ->
            val userRepository = UserRepositoryJdbi(handle)
            createUser()
            val foundUser = userRepository.findByUsername("user")

            assertNotNull(foundUser)
            assertEquals("user", foundUser.username)
        }
    }

    @Test
    fun `Test for User Not Found by Username`() {
        runWithHandle { handle ->
            val userRepository = UserRepositoryJdbi(handle)

            val foundUser = userRepository.findByUsername("not-user")
            assertNull(foundUser)
        }
    }

    @Test
    fun `Test for Valid Token Retrieval`() {
        runWithHandle { handle ->
            val userRepository = UserRepositoryJdbi(handle)
            val user = createUser()
            val now = Clock.System.now()

            val token = Token(TokenValidationInfo("token"), user.id, now, now)
            userRepository.createToken(token, 5)

            val retrievedPair =
                userRepository.getTokenByTokenValidationInfo(TokenValidationInfo("token"))

            assertNotNull(retrievedPair)
            assertEquals(user.id, retrievedPair.first.id)
        }
    }

    @Test
    fun `Test for Invalid Token Retrieval`() {
        runWithHandle { handle ->
            val userRepository = UserRepositoryJdbi(handle)

            val retrievedPair =
                userRepository.getTokenByTokenValidationInfo(TokenValidationInfo("token"))
            assertNull(retrievedPair)
        }
    }

    @Test
    fun `Test for Successful Token Creation and Old Token Deletion`() {
        runWithHandle { handle ->
            val userRepository = UserRepositoryJdbi(handle)
            val user = createUser()
            val now = Clock.System.now()

            val oldToken = Token(TokenValidationInfo("old-token"), user.id, now, now)
            userRepository.createToken(oldToken, 1)

            val newToken = Token(TokenValidationInfo("new-token"), user.id, now, now)
            userRepository.createToken(newToken, 1)

            val retrievedOldToken =
                userRepository.getTokenByTokenValidationInfo(TokenValidationInfo("old-token"))
            assertNull(retrievedOldToken)

            val retrievedNewToken =
                userRepository.getTokenByTokenValidationInfo(TokenValidationInfo("new-token"))
            assertNotNull(retrievedNewToken)
        }
    }

    @Test
    fun `Test for Token Update`() {
        runWithHandle { handle ->
            val userRepository = UserRepositoryJdbi(handle)
            val user = createUser()
            val now1 = Clock.System.now()

            val token = Token(TokenValidationInfo("token"), user.id, now1, now1)
            userRepository.createToken(token, 5)

            val now2 = Clock.System.now()
            userRepository.updateTokenLastUsed(token, now2)

            val updatedToken =
                userRepository.getTokenByTokenValidationInfo(TokenValidationInfo("token"))?.second
            assertNotNull(updatedToken)
            assertEquals(
                now2 - now2.nanosecondsOfSecond.toLong().nanoseconds,
                updatedToken.lastUsedAt - updatedToken.lastUsedAt.nanosecondsOfSecond.nanoseconds,
            )
        }
    }

    @Test
    fun `Test for Token Deletion`() {
        runWithHandle { handle ->
            val userRepository = UserRepositoryJdbi(handle)
            val user = createUser()
            val now = Clock.System.now()

            val token = Token(TokenValidationInfo("token"), user.id, now, now)
            userRepository.createToken(token, 5)

            val deletedCount =
                userRepository.removeTokenByValidationInfo(TokenValidationInfo("token"))
            assertEquals(1, deletedCount)

            val retrievedToken =
                userRepository.getTokenByTokenValidationInfo(TokenValidationInfo("token"))
            assertNull(retrievedToken)
        }
    }

    @Test
    fun `Test for Retrieving All Users`() {
        runWithHandle { handle ->
            val userRepository = UserRepositoryJdbi(handle)

            createUser("user1")
            createUser("user2")
            createUser("user3")

            val users = userRepository.findAll()
            assertEquals(3, users.size)
            assertTrue(users.any { it.username == "user1" })
            assertTrue(users.any { it.username == "user2" })
            assertTrue(users.any { it.username == "user3" })
        }
    }

    @Test
    fun `Test for Empty List of Users`() {
        runWithHandle { handle ->
            val userRepository = UserRepositoryJdbi(handle)

            val users = userRepository.findAll()
            assertTrue(users.isEmpty())
        }
    }

    @Test
    fun `Test for Successful User Update`() {
        runWithHandle { handle ->
            val userRepository = UserRepositoryJdbi(handle)
            val user = createUser()

            val updatedUser = user.copy(username = "updated-user")
            userRepository.save(updatedUser)

            val foundUser = userRepository.findById(user.id)
            assertNotNull(foundUser)
            assertEquals("updated-user", foundUser.username)
        }
    }

    @Test
    fun `Test for Successful Deletion by ID`() {
        runWithHandle { handle ->
            val userRepository = UserRepositoryJdbi(handle)
            val user = createUser()

            userRepository.deleteById(user.id)

            val foundUser = userRepository.findById(user.id)
            assertNull(foundUser)
        }
    }

    @Test
    fun `Test for Clearing All Users`() {
        runWithHandle { handle ->
            val userRepository = UserRepositoryJdbi(handle)

            createUser("user1")
            createUser("user2")

            userRepository.clear()

            val users = userRepository.findAll()
            assertTrue(users.isEmpty())
        }
    }
}
