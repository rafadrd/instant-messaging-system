package pt.isel.repositories.users

import pt.isel.domain.security.PasswordValidationInfo
import pt.isel.domain.users.User
import pt.isel.repositories.Repository

/** Repository interface for managing users, extends the generic Repository */
interface UserRepository : Repository<User> {
    fun create(
        username: String,
        passwordValidationInfo: PasswordValidationInfo,
    ): User

    fun findByUsername(username: String): User?

    fun hasUsers(): Boolean
}
