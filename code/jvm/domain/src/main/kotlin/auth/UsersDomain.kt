package pt.isel.auth

import jakarta.inject.Named
import org.springframework.security.crypto.password.PasswordEncoder

@Named
class UsersDomain(
    private val passwordEncoder: PasswordEncoder,
) {
    fun validatePassword(
        password: String,
        validationInfo: PasswordValidationInfo,
    ): Boolean = passwordEncoder.matches(password, validationInfo.validationInfo)

    fun createPasswordValidationInformation(password: String): PasswordValidationInfo =
        PasswordValidationInfo(passwordEncoder.encode(password))

    fun isSafePassword(password: String): Boolean {
        if (password.length < 8) return false
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        return hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar
    }
}
