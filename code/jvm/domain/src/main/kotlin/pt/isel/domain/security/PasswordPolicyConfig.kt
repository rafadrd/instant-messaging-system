package pt.isel.domain.security

data class PasswordPolicyConfig(
    val minLength: Int,
    val requiresUppercase: Boolean,
    val requiresLowercase: Boolean,
    val requiresDigit: Boolean,
    val requiresSpecialChar: Boolean,
)
