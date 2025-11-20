package pt.isel

import io.github.cdimascio.dotenv.dotenv
import kotlinx.datetime.Clock
import org.jdbi.v3.core.Jdbi
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
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
    @Value("\${cors.allowed-origins}") val allowedOrigins: String,
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
            .allowedOrigins(*allowedOrigins.split(",").toTypedArray())
            .allowCredentials(true)
    }
}

@SpringBootApplication(exclude = [DataSourceAutoConfiguration::class])
class AppInstantMessaging {
    @Bean
    fun jdbi(
        @Value("\${spring.datasource.url}") dbUrl: String,
    ) = Jdbi
        .create(PGSimpleDataSource().apply { setURL(dbUrl) })
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
    fun sseExecutor(
        @Value("\${sse.pool.core-size}") coreSize: Int,
        @Value("\${sse.pool.max-size}") maxSize: Int,
        @Value("\${sse.pool.queue-capacity}") queueCapacity: Int,
    ): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = coreSize
        executor.maxPoolSize = maxSize
        executor.queueCapacity = queueCapacity
        executor.threadNamePrefix = "sse-broadcaster-"
        executor.initialize()
        return executor
    }
}

fun main(args: Array<String>) {
    val dotenv =
        dotenv {
            directory = "./"
            ignoreIfMissing = true
        }
    dotenv.entries().forEach { System.setProperty(it.key, it.value) }
    runApplication<AppInstantMessaging>(*args)
}
