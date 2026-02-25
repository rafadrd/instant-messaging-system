package pt.isel.domain

import pt.isel.domain.auth.PasswordValidationInfo

data class User(
    val id: Long,
    val username: String,
    val passwordValidation: PasswordValidationInfo,
)
