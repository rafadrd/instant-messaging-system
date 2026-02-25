package pt.isel.repositories

import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertNull
import pt.isel.auth.Token
import pt.isel.auth.TokenValidationInfo
import pt.isel.domain.auth.PasswordValidationInfo
import pt.isel.mem.UserRepositoryInMem
import kotlin.test.Test
import kotlin.test.assertEquals

const val PASSWORD = "Aa1#2345"

class UserRepositoryTest {
    private val repo = UserRepositoryInMem()

    @Test
    fun `create user`() {
        val newUser = repo.create("AntonioBanderas", PasswordValidationInfo(PASSWORD))
        assertEquals(1, newUser.id)
        assertEquals("AntonioBanderas", newUser.username)
        assertEquals(PASSWORD, newUser.passwordValidation.validationInfo)
    }

    @Test
    fun `create more users`() {
        val newUser = repo.create("BradPit", PasswordValidationInfo("1234"))
        val newUser2 = repo.create("AngelinaJolie", PasswordValidationInfo("1234"))
        assertEquals(1, newUser.id)
        assertEquals("BradPit", newUser.username)
        assertEquals("1234", newUser.passwordValidation.validationInfo)
        assertEquals(2, newUser2.id)
        assertEquals("AngelinaJolie", newUser2.username)
        assertEquals("1234", newUser2.passwordValidation.validationInfo)
    }

    @Test
    fun `find user by id`() {
        val newUser = repo.create("AntonioBanderas", PasswordValidationInfo(PASSWORD))
        val foundUser = repo.findById(1)
        assertEquals(newUser, foundUser)
    }

    @Test
    fun `find user by username`() {
        val newUser = repo.create("AntonioBanderas", PasswordValidationInfo(PASSWORD))
        val foundUser = repo.findByUsername("AntonioBanderas")
        assertEquals(newUser, foundUser)
    }

    @Test
    fun `find all users`() {
        val newUser = repo.create("AntonioBanderas", PasswordValidationInfo(PASSWORD))
        val newUser2 = repo.create("BradPit", PasswordValidationInfo(PASSWORD))
        val newUser3 = repo.create("AngelinaJolie", PasswordValidationInfo(PASSWORD))
        val allUsers = repo.findAll()
        assertEquals(listOf(newUser, newUser2, newUser3), allUsers)
    }

    @Test
    fun `save user`() {
        val newUser = repo.create("AntonioBanderas", PasswordValidationInfo(PASSWORD))
        val updatedUser = newUser.copy(username = "AntonioBanderas2")
        repo.save(updatedUser)
        assertEquals(updatedUser, repo.findById(1))
    }

    @Test
    fun `save one or more users`() {
        val newUser = repo.create("AntonioBanderas", PasswordValidationInfo(PASSWORD))
        val newUser2 = repo.create("BradPit", PasswordValidationInfo(PASSWORD))
        val newUser3 = repo.create("AngelinaJolie", PasswordValidationInfo(PASSWORD))
        val updatedUser = newUser.copy(username = "AntonioBanderas2")
        val updatedUser2 = newUser2.copy(username = "BradPit2")
        repo.save(updatedUser)
        repo.save(updatedUser2)
        assertEquals(updatedUser, repo.findById(1))
        assertEquals(updatedUser2, repo.findById(2))
        assertEquals(newUser3, repo.findById(3))
    }

    @Test
    fun `delete user by id`() {
        repo.create("AntonioBanderas", PasswordValidationInfo(PASSWORD))
        repo.deleteById(1)
        assertEquals(null, repo.findById(1))
    }

    @Test
    fun `delete one or more users`() {
        repo.create("AntonioBanderas", PasswordValidationInfo(PASSWORD))
        repo.create("BradPit", PasswordValidationInfo(PASSWORD))
        val newUser3 = repo.create("AngelinaJolie", PasswordValidationInfo(PASSWORD))
        repo.deleteById(1)
        repo.deleteById(2)
        assertEquals(listOf(newUser3), repo.findAll())
    }

    @Test
    fun `clear all users`() {
        repo.create("AntonioBanderas", PasswordValidationInfo(PASSWORD))
        repo.create("BradPit", PasswordValidationInfo(PASSWORD))
        repo.create("AngelinaJolie", PasswordValidationInfo(PASSWORD))
        repo.clear()
        assertEquals(emptyList(), repo.findAll())
    }

    @Test
    fun `get token by token validation info`() {
        val newUser = repo.create("AntonioBanderas", PasswordValidationInfo(PASSWORD))
        val token =
            Token(TokenValidationInfo("token1"), newUser.id, Clock.System.now(), Clock.System.now())
        repo.createToken(token, 5)
        val result = repo.getTokenByTokenValidationInfo(TokenValidationInfo("token1"))
        assertEquals(newUser to token, result)
    }

    @Test
    fun `create token`() {
        val newUser = repo.create("AntonioBanderas", PasswordValidationInfo(PASSWORD))
        val token =
            Token(TokenValidationInfo("token1"), newUser.id, Clock.System.now(), Clock.System.now())
        repo.createToken(token, 5)
        val result = repo.getTokenByTokenValidationInfo(TokenValidationInfo("token1"))
        assertEquals(newUser to token, result)
    }

    @Test
    fun `update token last used`() {
        val newUser = repo.create("AntonioBanderas", PasswordValidationInfo(PASSWORD))
        val token =
            Token(TokenValidationInfo("token1"), newUser.id, Clock.System.now(), Clock.System.now())
        repo.createToken(token, 5)
        val newTime = Clock.System.now()
        val updatedToken = Token(token.tokenValidationInfo, token.userId, token.createdAt, newTime)
        repo.updateTokenLastUsed(updatedToken, newTime)
        val result = repo.getTokenByTokenValidationInfo(TokenValidationInfo("token1"))
        assertEquals(newUser to updatedToken, result)
    }

    @Test
    fun `remove token by validation info`() {
        val newUser = repo.create("AntonioBanderas", PasswordValidationInfo(PASSWORD))
        val token =
            Token(TokenValidationInfo("token1"), newUser.id, Clock.System.now(), Clock.System.now())
        repo.createToken(token, 5)
        val count = repo.removeTokenByValidationInfo(TokenValidationInfo("token1"))
        assertEquals(1, count)
        val result = repo.getTokenByTokenValidationInfo(TokenValidationInfo("token1"))
        assertNull(result)
    }
}
