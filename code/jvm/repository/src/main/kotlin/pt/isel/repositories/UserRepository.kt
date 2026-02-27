package pt.isel.repositories

import pt.isel.domain.security.PasswordValidationInfo
import pt.isel.domain.user.User

/** Repository interface for managing users, extends the generic Repository */
interface UserRepository : Repository<User> {
    fun create(
        username: String,
        passwordValidationInfo: PasswordValidationInfo,
    ): User

    fun findByUsername(username: String): User?

    fun hasUsers(): Boolean
}
