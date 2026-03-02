package pt.isel.services.users

import jakarta.inject.Named
import org.springframework.scheduling.annotation.Scheduled
import pt.isel.domain.common.Either
import pt.isel.domain.common.Failure
import pt.isel.domain.common.Success
import pt.isel.domain.common.UserError
import pt.isel.domain.common.failure
import pt.isel.domain.common.success
import pt.isel.domain.invitations.Invitation
import pt.isel.domain.invitations.InvitationStatus
import pt.isel.domain.security.PasswordSecurityDomain
import pt.isel.domain.security.TokenExternalInfo
import pt.isel.domain.users.User
import pt.isel.domain.users.UserInfo
import pt.isel.repositories.Transaction
import pt.isel.repositories.TransactionManager
import java.time.Clock
import java.time.LocalDateTime

@Named
class UserService(
    private val trxManager: TransactionManager,
    private val passwordSecurityDomain: PasswordSecurityDomain,
    private val tokenService: TokenService,
    private val clock: Clock,
) {
    fun registerUser(
        username: String,
        password: String,
        token: String? = null,
    ): Either<UserError, TokenExternalInfo> =
        trxManager.run {
            var invitation: Invitation? = null

            if (!token.isNullOrBlank()) {
                invitation = repoInvitations.findByToken(token)
                    ?: return@run failure(UserError.InvitationNotFound)

                if (invitation.expiresAt.isBefore(LocalDateTime.now(clock))) {
                    return@run failure(UserError.InvitationExpired)
                }
                if (invitation.status != InvitationStatus.PENDING) {
                    return@run failure(UserError.InvitationAlreadyUsed)
                }
            }

            val newUserResult = createUser(username, password)
            if (newUserResult is Failure) return@run newUserResult

            val user = (newUserResult as Success).value

            if (invitation != null) {
                repoMemberships.addUserToChannel(
                    UserInfo(user.id, user.username),
                    invitation.channel,
                    invitation.accessType,
                )
                repoInvitations.save(invitation.copy(status = InvitationStatus.ACCEPTED))
            }

            success(tokenService.createToken(user.id))
        }

    private fun Transaction.createUser(
        username: String,
        password: String,
    ): Either<UserError, User> {
        if (username.isBlank()) return failure(UserError.EmptyUsername)
        if (username.length !in 1..30) return failure(UserError.InvalidUsernameLength)
        if (password.isBlank()) return failure(UserError.EmptyPassword)

        if (repoUsers.findByUsername(username) != null) {
            return failure(UserError.UsernameAlreadyInUse)
        }
        if (!passwordSecurityDomain.isSafePassword(password)) {
            return failure(UserError.InsecurePassword)
        }

        val passwordValidationInfo = passwordSecurityDomain.createPasswordValidationInformation(password)
        val newUser = repoUsers.create(username, passwordValidationInfo)

        return success(newUser)
    }

    fun getUserById(userId: Long): Either<UserError, User> =
        trxManager.run {
            repoUsers.findById(userId)?.let { success(it) }
                ?: return@run failure(UserError.UserNotFound)
        }

    fun getUserByUsername(username: String): Either<UserError, User> {
        if (username.isBlank()) return failure(UserError.EmptyUsername)

        return trxManager.run {
            repoUsers.findByUsername(username)?.let { success(it) }
                ?: return@run failure(UserError.UserNotFound)
        }
    }

    fun updateUsername(
        userId: Long,
        newUsername: String,
        password: String,
    ): Either<UserError, User> {
        if (newUsername.isBlank()) return failure(UserError.EmptyUsername)
        if (newUsername.length !in 1..30) return failure(UserError.InvalidUsernameLength)

        return trxManager.run {
            val user = repoUsers.findById(userId) ?: return@run failure(UserError.UserNotFound)

            val existing = repoUsers.findByUsername(newUsername)
            if (existing != null && existing.id != userId) {
                return@run failure(UserError.UsernameAlreadyInUse)
            }
            if (!passwordSecurityDomain.validatePassword(password, user.passwordValidation)) {
                return@run failure(UserError.IncorrectPassword)
            }

            val updatedUser = user.copy(username = newUsername)
            repoUsers.save(updatedUser)
            success(updatedUser)
        }
    }

    fun updatePassword(
        userId: Long,
        oldPassword: String,
        newPassword: String,
    ): Either<UserError, User> {
        if (newPassword.isBlank()) return failure(UserError.EmptyPassword)
        if (!passwordSecurityDomain.isSafePassword(newPassword)) {
            return failure(UserError.InsecurePassword)
        }

        return trxManager.run {
            val user = repoUsers.findById(userId) ?: return@run failure(UserError.UserNotFound)

            if (!passwordSecurityDomain.validatePassword(oldPassword, user.passwordValidation)) {
                return@run failure(UserError.IncorrectPassword)
            }

            if (passwordSecurityDomain.validatePassword(newPassword, user.passwordValidation)) {
                return@run failure(UserError.PasswordSameAsPrevious)
            }

            val passwordValidation = passwordSecurityDomain.createPasswordValidationInformation(newPassword)
            val updatedUser = user.copy(passwordValidation = passwordValidation)
            repoUsers.save(updatedUser)
            success(updatedUser)
        }
    }

    fun deleteUser(userId: Long): Either<UserError, String> =
        trxManager.run {
            val user = repoUsers.findById(userId) ?: return@run failure(UserError.UserNotFound)

            if (repoChannels.findAllByOwner(user.id).isNotEmpty()) {
                return@run failure(UserError.UserHasOwnedChannels)
            }

            repoMemberships.removeAllMembershipsForUser(user.id)
            repoUsers.deleteById(userId)
            success("User ${user.id} deleted")
        }

    fun createToken(
        username: String,
        password: String,
    ): Either<UserError, TokenExternalInfo> {
        if (username.isBlank()) return failure(UserError.EmptyUsername)
        if (password.isBlank()) return failure(UserError.EmptyPassword)

        return trxManager.run {
            val user =
                repoUsers.findByUsername(username) ?: return@run failure(UserError.UserNotFound)

            if (!passwordSecurityDomain.validatePassword(password, user.passwordValidation)) {
                return@run failure(UserError.IncorrectPassword)
            }

            val tokenInfo = tokenService.createToken(user.id)
            success(tokenInfo)
        }
    }

    fun getUserByToken(token: String): User? {
        val parsedToken = tokenService.validateToken(token) ?: return null

        return trxManager.run {
            if (repoTokenBlacklist.exists(parsedToken.jti)) {
                return@run null
            }
            repoUsers.findById(parsedToken.userId)
        }
    }

    fun revokeToken(token: String) {
        val parsedToken = tokenService.validateToken(token) ?: return
        trxManager.run {
            repoTokenBlacklist.add(parsedToken.jti, parsedToken.expiresAt)
        }
    }

    @Scheduled(fixedRate = 3600000) // Cleanup every 1 hour
    fun cleanupExpiredTokens() {
        trxManager.run {
            repoTokenBlacklist.cleanupExpired()
        }
    }
}
