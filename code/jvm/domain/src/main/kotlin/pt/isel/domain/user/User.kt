package pt.isel.domain.user

import pt.isel.domain.security.PasswordValidationInfo

data class User(
    val id: Long,
    val username: String,
    val passwordValidation: PasswordValidationInfo,
)
