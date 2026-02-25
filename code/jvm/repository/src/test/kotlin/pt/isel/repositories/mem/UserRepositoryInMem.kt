package pt.isel.repositories.mem

import jakarta.inject.Named
import pt.isel.domain.User
import pt.isel.domain.auth.PasswordValidationInfo
import pt.isel.repositories.UserRepository

/**
 * Naif in memory repository non thread-safe and basic sequential id. Useful for unit tests purpose.
 */
@Named
class UserRepositoryInMem : UserRepository {
    private val users = mutableListOf<User>()

    override fun create(
        username: String,
        passwordValidationInfo: PasswordValidationInfo,
    ): User = User(users.size.toLong() + 1, username, passwordValidationInfo).also { users.add(it) }

    override fun findById(id: Long): User? = users.firstOrNull { it.id == id }

    override fun findByUsername(username: String): User? = users.firstOrNull { it.username == username }

    override fun hasUsers(): Boolean = users.isNotEmpty()

    override fun findAll(): List<User> = users.toList()

    override fun save(entity: User) {
        users.removeIf { it.id == entity.id }.apply { users.add(entity) }
    }

    override fun deleteById(id: Long) {
        users.removeIf { it.id == id }
    }

    override fun clear() {
        users.clear()
    }
}
