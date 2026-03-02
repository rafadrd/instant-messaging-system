package pt.isel.pipeline.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import pt.isel.pipeline.authentication.AuthenticatedUserArgumentResolver;
import pt.isel.pipeline.authentication.AuthenticationInterceptor;

import java.util.List;

@Configuration
public class PipelineConfigurer implements WebMvcConfigurer {

    private final AuthenticationInterceptor authenticationInterceptor;
    private final AuthenticatedUserArgumentResolver authenticatedUserArgumentResolver;
    private final String allowedOrigins;

    public PipelineConfigurer(
            AuthenticationInterceptor authenticationInterceptor,
            AuthenticatedUserArgumentResolver authenticatedUserArgumentResolver,
            @Value("${cors.allowed-origins}") String allowedOrigins) {
        this.authenticationInterceptor = authenticationInterceptor;
        this.authenticatedUserArgumentResolver = authenticatedUserArgumentResolver;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authenticationInterceptor);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authenticatedUserArgumentResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true);
    }
}