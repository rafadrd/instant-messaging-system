package pt.isel.auth

import org.springframework.stereotype.Component
import org.springframework.security.crypto.password.PasswordEncoder as SpringPasswordEncoder

@Component
class SpringPasswordEncoderAdapter(
    private val springEncoder: SpringPasswordEncoder,
) : PasswordEncoder {
    override fun encode(rawPassword: String): String = springEncoder.encode(rawPassword)

    override fun matches(
        rawPassword: String,
        encodedPassword: String,
    ): Boolean = springEncoder.matches(rawPassword, encodedPassword)
}
