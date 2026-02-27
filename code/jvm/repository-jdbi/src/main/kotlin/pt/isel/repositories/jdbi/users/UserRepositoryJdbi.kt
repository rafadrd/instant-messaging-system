package pt.isel.repositories.jdbi.users

import org.jdbi.v3.core.Handle
import org.slf4j.LoggerFactory
import pt.isel.domain.security.PasswordValidationInfo
import pt.isel.domain.users.User
import pt.isel.repositories.jdbi.utils.executeQueryToList
import pt.isel.repositories.jdbi.utils.executeQueryToSingle
import pt.isel.repositories.jdbi.utils.executeUpdate
import pt.isel.repositories.jdbi.utils.executeUpdateAndReturnId
import pt.isel.repositories.users.UserRepository
import java.sql.ResultSet

class UserRepositoryJdbi(
    private val handle: Handle,
) : UserRepository {
    override fun create(
        username: String,
        passwordValidationInfo: PasswordValidationInfo,
    ): User {
        val id =
            handle.executeUpdateAndReturnId(
                """
                INSERT INTO dbo.users (username, password_validation)
                VALUES (:username, :password_validation)
                """,
                mapOf(
                    "username" to username,
                    "password_validation" to passwordValidationInfo.validationInfo,
                ),
            )
        return User(id, username, passwordValidationInfo)
    }

    override fun findById(id: Long): User? =
        handle.executeQueryToSingle(
            "SELECT * FROM dbo.users WHERE id = :id",
            mapOf("id" to id),
            ::mapRowToUser,
        )

    override fun findByUsername(username: String): User? =
        handle.executeQueryToSingle(
            "SELECT * FROM dbo.users WHERE username = :username",
            mapOf("username" to username),
            ::mapRowToUser,
        )

    override fun hasUsers(): Boolean =
        handle
            .createQuery("SELECT 1 FROM dbo.users LIMIT 1")
            .mapTo(Int::class.java)
            .findOne()
            .isPresent

    override fun findAll(): List<User> =
        handle.executeQueryToList(
            "SELECT * FROM dbo.users",
            mapper = ::mapRowToUser,
        )

    override fun save(entity: User) {
        handle.executeUpdate(
            """
            UPDATE dbo.users
            SET username = :username, password_validation = :password_validation
            WHERE id = :id
            """,
            mapOf(
                "id" to entity.id,
                "username" to entity.username,
                "password_validation" to entity.passwordValidation.validationInfo,
            ),
        )
    }

    override fun deleteById(id: Long) {
        handle.executeUpdate("DELETE FROM dbo.users WHERE id = :id", mapOf("id" to id))
    }

    override fun clear() {
        handle.executeUpdate("DELETE FROM dbo.users")
    }

    private fun mapRowToUser(rs: ResultSet): User =
        User(
            rs.getLong("id"),
            rs.getString("username"),
            PasswordValidationInfo(rs.getString("password_validation")),
        )

    companion object {
        private val logger = LoggerFactory.getLogger(UserRepositoryJdbi::class.java)
    }
}
