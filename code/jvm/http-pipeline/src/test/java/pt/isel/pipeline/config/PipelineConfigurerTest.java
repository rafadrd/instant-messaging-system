package pt.isel.pipeline.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import pt.isel.pipeline.authentication.AuthenticatedUserArgumentResolver;
import pt.isel.pipeline.authentication.AuthenticationInterceptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PipelineConfigurerTest {

    private AuthenticationInterceptor interceptor;
    private AuthenticatedUserArgumentResolver resolver;
    private PipelineConfigurer configurer;

    @BeforeEach
    void setUp() {
        interceptor = mock(AuthenticationInterceptor.class);
        resolver = mock(AuthenticatedUserArgumentResolver.class);
        configurer = new PipelineConfigurer(interceptor, resolver, "http://localhost:3000,http://localhost:8000");
    }

    @Test
    void testAddInterceptors() {
        InterceptorRegistry registry = mock(InterceptorRegistry.class);

        configurer.addInterceptors(registry);

        verify(registry).addInterceptor(interceptor);
    }

    @Test
    void testAddArgumentResolvers() {
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

        configurer.addArgumentResolvers(resolvers);

        assertEquals(1, resolvers.size());
        assertTrue(resolvers.contains(resolver));
    }

    @Test
    void testAddCorsMappings() {
        CorsRegistry registry = mock(CorsRegistry.class);
        CorsRegistration registration = mock(CorsRegistration.class);

        when(registry.addMapping(anyString())).thenReturn(registration);
        when(registration.allowedOrigins(org.mockito.ArgumentMatchers.any(String[].class))).thenReturn(registration);
        when(registration.allowedMethods(org.mockito.ArgumentMatchers.any(String[].class))).thenReturn(registration);
        when(registration.allowCredentials(true)).thenReturn(registration);

        configurer.addCorsMappings(registry);

        verify(registry).addMapping("/api/**");
        verify(registration).allowedOrigins("http://localhost:3000", "http://localhost:8000");
        verify(registration).allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
        verify(registration).allowCredentials(true);
    }
}