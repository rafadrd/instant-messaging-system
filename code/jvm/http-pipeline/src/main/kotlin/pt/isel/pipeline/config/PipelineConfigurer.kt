package pt.isel.pipeline.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import pt.isel.pipeline.authentication.AuthenticatedUserArgumentResolver
import pt.isel.pipeline.authentication.AuthenticationInterceptor

@Configuration
class PipelineConfigurer(
    private val authenticationInterceptor: AuthenticationInterceptor,
    private val authenticatedUserArgumentResolver: AuthenticatedUserArgumentResolver,
    @Value($$"${cors.allowed-origins}") private val allowedOrigins: String,
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
