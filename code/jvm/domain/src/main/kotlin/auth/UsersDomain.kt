package pt.isel.auth

import jakarta.inject.Named

@Named
class UsersDomain(
    private val passwordEncoder: PasswordEncoder,
    private val passwordPolicyConfig: PasswordPolicyConfig,
) {
    fun validatePassword(
        password: String,
        validationInfo: PasswordValidationInfo,
    ): Boolean = passwordEncoder.matches(password, validationInfo.validationInfo)

    fun createPasswordValidationInformation(password: String): PasswordValidationInfo =
        PasswordValidationInfo(passwordEncoder.encode(password))

    fun isSafePassword(password: String): Boolean {
        if (password.length < passwordPolicyConfig.minLength) return false
        if (passwordPolicyConfig.requiresUppercase && !password.any { it.isUpperCase() }) return false
        if (passwordPolicyConfig.requiresLowercase && !password.any { it.isLowerCase() }) return false
        if (passwordPolicyConfig.requiresDigit && !password.any { it.isDigit() }) return false
        if (passwordPolicyConfig.requiresSpecialChar && !password.any { !it.isLetterOrDigit() }) return false
        return true
    }
}
