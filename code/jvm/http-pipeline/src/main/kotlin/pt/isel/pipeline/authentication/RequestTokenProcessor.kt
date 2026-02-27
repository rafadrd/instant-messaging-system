package pt.isel.pipeline.authentication

import org.springframework.stereotype.Component
import pt.isel.domain.users.AuthenticatedUser
import pt.isel.services.users.UserService

@Component
class RequestTokenProcessor(
    private val usersService: UserService,
) {
    fun processAuthorizationHeaderValue(authorizationValue: String?): AuthenticatedUser? {
        if (authorizationValue.isNullOrBlank()) return null

        val parts = authorizationValue.split(' ').filter { it.isNotBlank() }
        if (parts.size != 2 || !parts[0].equals(SCHEME, ignoreCase = true)) return null

        val token = parts[1]
        return usersService.getUserByToken(token)?.let { AuthenticatedUser(it, token) }
    }

    companion object {
        const val SCHEME = "bearer"
    }
}
