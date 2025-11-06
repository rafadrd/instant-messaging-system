package pt.isel

import kotlinx.datetime.Clock
import org.jdbi.v3.core.Jdbi
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import pt.isel.auth.PasswordPolicyConfig
import java.util.concurrent.Executor

@Configuration
class PipelineConfigurer(
    val authenticationInterceptor: AuthenticationInterceptor,
    val authenticatedUserArgumentResolver: AuthenticatedUserArgumentResolver,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(authenticationInterceptor)
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(authenticatedUserArgumentResolver)
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        registry
            .addMapping("/api/**")
            .allowedOrigins("http://localhost:8000")
            .allowCredentials(true)
    }
}

@SpringBootApplication
class AppInstantMessaging {
    @Bean
    fun jdbi() =
        Jdbi
            .create(PGSimpleDataSource().apply { setURL(Environment.getDbUrl()) })
            .configureWithAppRequirements()

    @Bean
    @Profile("jdbi")
    fun trxManagerJdbi(jdbi: Jdbi) = TransactionManagerJdbi(jdbi)

    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()

    @Bean
    fun clock() = Clock.System

    @Bean
    fun passwordPolicyConfig(
        @Value("\${password.policy.min-length}") minLength: Int,
        @Value("\${password.policy.requires-uppercase}") requiresUppercase: Boolean,
        @Value("\${password.policy.requires-lowercase}") requiresLowercase: Boolean,
        @Value("\${password.policy.requires-digit}") requiresDigit: Boolean,
        @Value("\${password.policy.requires-special-char}") requiresSpecialChar: Boolean,
    ): PasswordPolicyConfig =
        PasswordPolicyConfig(
            minLength = minLength,
            requiresUppercase = requiresUppercase,
            requiresLowercase = requiresLowercase,
            requiresDigit = requiresDigit,
            requiresSpecialChar = requiresSpecialChar,
        )

    @Bean
    fun sseExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 10
        executor.maxPoolSize = 50
        executor.queueCapacity = 100
        executor.threadNamePrefix = "sse-broadcaster-"
        executor.initialize()
        return executor
    }
}

fun main() {
    runApplication<AppInstantMessaging>()
}
