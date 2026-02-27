package pt.isel.host

import org.jdbi.v3.core.Jdbi
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pt.isel.domain.security.PasswordPolicyConfig
import pt.isel.repositories.jdbi.TransactionManagerJdbi
import pt.isel.repositories.jdbi.configureWithAppRequirements
import java.time.Clock
import javax.sql.DataSource

@SpringBootApplication(scanBasePackages = ["pt.isel"])
@EnableScheduling
class AppInstantMessaging {
    @Bean
    fun jdbi(dataSource: DataSource) =
        Jdbi
            .create(dataSource)
            .configureWithAppRequirements()

    @Bean
    @Profile("jdbi")
    fun trxManagerJdbi(jdbi: Jdbi) = TransactionManagerJdbi(jdbi)

    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()

    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun passwordPolicyConfig(
        @Value($$"${password.policy.min-length}") minLength: Int,
        @Value($$"${password.policy.requires-uppercase}") requiresUppercase: Boolean,
        @Value($$"${password.policy.requires-lowercase}") requiresLowercase: Boolean,
        @Value($$"${password.policy.requires-digit}") requiresDigit: Boolean,
        @Value($$"${password.policy.requires-special-char}") requiresSpecialChar: Boolean,
    ): PasswordPolicyConfig =
        PasswordPolicyConfig(
            minLength = minLength,
            requiresUppercase = requiresUppercase,
            requiresLowercase = requiresLowercase,
            requiresDigit = requiresDigit,
            requiresSpecialChar = requiresSpecialChar,
        )
}

fun main(args: Array<String>) {
    runApplication<AppInstantMessaging>(*args)
}
