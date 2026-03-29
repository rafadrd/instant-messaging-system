package pt.isel.pipeline.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import pt.isel.pipeline.authentication.AuthenticatedUserArgumentResolver;
import pt.isel.pipeline.authentication.AuthenticationInterceptor;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PipelineConfigurerTest {

    @Mock
    private AuthenticationInterceptor interceptor;

    @Mock
    private AuthenticatedUserArgumentResolver resolver;

    private PipelineConfigurer configurer;

    @BeforeEach
    void setUp() {
        configurer = new PipelineConfigurer(interceptor, resolver, "http://localhost:3000,http://localhost:8000");
    }

    @Test
    void AddInterceptors_ValidRegistry_AddsInterceptor() {
        InterceptorRegistry registry = mock(InterceptorRegistry.class);

        configurer.addInterceptors(registry);

        verify(registry).addInterceptor(interceptor);
    }

    @Test
    void AddArgumentResolvers_ValidList_AddsResolver() {
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

        configurer.addArgumentResolvers(resolvers);

        assertThat(resolvers).hasSize(1).contains(resolver);
    }

    @Test
    void AddCorsMappings_ValidRegistry_AddsMappings() {
        CorsRegistry registry = mock(CorsRegistry.class);
        CorsRegistration registration = mock(CorsRegistration.class);

        when(registry.addMapping(anyString())).thenReturn(registration);
        when(registration.allowedOrigins(any(String[].class))).thenReturn(registration);
        when(registration.allowedMethods(any(String[].class))).thenReturn(registration);
        when(registration.allowCredentials(true)).thenReturn(registration);

        configurer.addCorsMappings(registry);

        verify(registry).addMapping("/api/**");
        verify(registration).allowedOrigins("http://localhost:3000", "http://localhost:8000");
        verify(registration).allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
        verify(registration).allowCredentials(true);
    }
}