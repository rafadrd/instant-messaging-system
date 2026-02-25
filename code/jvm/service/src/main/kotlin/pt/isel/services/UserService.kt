package pt.isel.services

import jakarta.inject.Named
import pt.isel.domain.Status
import pt.isel.domain.User
import pt.isel.domain.UserInfo
import pt.isel.domain.auth.PasswordSecurityDomain
import pt.isel.domain.auth.TokenExternalInfo
import pt.isel.repositories.Transaction
import pt.isel.repositories.TransactionManager
import java.time.LocalDateTime

@Named
class UserService(
    private val trxManager: TransactionManager,
    private val passwordSecurityDomain: PasswordSecurityDomain,
    private val tokenService: TokenService,
) {
    fun registerUser(
        username: String,
        password: String,
        token: String? = null,
    ): Either<UserError, TokenExternalInfo> =
        trxManager.run {
            val userEither =
                if (token.isNullOrBlank()) {
                    // The check for the first user has been removed to allow open registration.
                    // A different policy (e.g., admin approval) could be implemented here if needed.
                    createUser(username, password)
                } else {
                    val invitation =
                        repoInvitations.findByToken(token)
                            ?: return@run failure(UserError.InvitationNotFound)

                    if (invitation.expiresAt.isBefore(LocalDateTime.now())) {
                        return@run failure(UserError.InvitationExpired)
                    }
                    if (invitation.status != Status.PENDING) {
                        return@run failure(UserError.InvitationAlreadyUsed)
                    }

                    val newUserResult = createUser(username, password)
                    if (newUserResult is Failure) {
                        return@run newUserResult
                    }

                    val newUser = (newUserResult as Success).value
                    val newUserInfo = UserInfo(newUser.id, newUser.username)
                    repoMemberships.addUserToChannel(
                        newUserInfo,
                        invitation.channel,
                        invitation.accessType,
                    )
                    repoInvitations.save(invitation.copy(status = Status.ACCEPTED))
                    newUserResult
                }

            when (userEither) {
                is Failure -> {
                    userEither
                }

                is Success -> {
                    val tokenInfo = tokenService.createToken(userEither.value.id)
                    success(tokenInfo)
                }
            }
        }

    private fun Transaction.createUser(
        username: String,
        password: String,
    ): Either<UserError, User> {
        if (username.isBlank()) return failure(UserError.EmptyUsername)
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

        return trxManager.run {
            val user = repoUsers.findById(userId) ?: return@run failure(UserError.UserNotFound)

            if (repoUsers.findByUsername(newUsername) != null) {
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
        newPassword: String,
    ): Either<UserError, User> {
        if (newPassword.isBlank()) return failure(UserError.EmptyPassword)
        if (!passwordSecurityDomain.isSafePassword(newPassword)) {
            return failure(UserError.InsecurePassword)
        }

        return trxManager.run {
            val user = repoUsers.findById(userId) ?: return@run failure(UserError.UserNotFound)

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

            repoMemberships.findAllChannelsForUser(user.id, Int.MAX_VALUE, 0).forEach {
                repoMemberships.removeUserFromChannel(user.id, it.channel.id)
            }

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
}
