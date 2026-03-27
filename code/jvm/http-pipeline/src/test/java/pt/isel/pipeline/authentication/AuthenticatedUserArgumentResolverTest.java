package pt.isel.pipeline.authentication;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import pt.isel.domain.builders.UserBuilder;
import pt.isel.domain.users.AuthenticatedUser;
import pt.isel.domain.users.User;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticatedUserArgumentResolverTest {

    private AuthenticatedUserArgumentResolver resolver;
    private MethodParameter validParameter;
    private MethodParameter invalidParameter;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        resolver = new AuthenticatedUserArgumentResolver();

        Method validMethod = DummyController.class.getMethod("handleWithAuth", AuthenticatedUser.class);
        validParameter = new MethodParameter(validMethod, 0);

        Method invalidMethod = DummyController.class.getMethod("handleWithoutAuth", String.class);
        invalidParameter = new MethodParameter(invalidMethod, 0);
    }

    @Test
    void testSupportsParameter() {
        assertThat(resolver.supportsParameter(validParameter)).isTrue();
        assertThat(resolver.supportsParameter(invalidParameter)).isFalse();
    }

    @Test
    void testResolveArgumentSuccess() {
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        ModelAndViewContainer mavContainer = mock(ModelAndViewContainer.class);

        User user = new UserBuilder().withId(1L).withUsername("alice").build();
        AuthenticatedUser authUser = new AuthenticatedUser(user, "token123");

        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(request);
        when(request.getAttribute("AuthenticatedUserArgumentResolver")).thenReturn(authUser);

        Object result = resolver.resolveArgument(validParameter, mavContainer, webRequest, null);

        assertThat(result).isEqualTo(authUser);
    }

    @Test
    void testResolveArgumentThrowsWhenRequestIsNull() {
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        ModelAndViewContainer mavContainer = mock(ModelAndViewContainer.class);

        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(null);

        assertThatThrownBy(() -> resolver.resolveArgument(validParameter, mavContainer, webRequest, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to extract HttpServletRequest from NativeWebRequest.");
    }

    @Test
    void testResolveArgumentThrowsWhenUserNotFoundInAttributes() {
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        ModelAndViewContainer mavContainer = mock(ModelAndViewContainer.class);

        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(request);
        when(request.getAttribute("AuthenticatedUserArgumentResolver")).thenReturn(null);

        assertThatThrownBy(() -> resolver.resolveArgument(validParameter, mavContainer, webRequest, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("AuthenticatedUser not found in request attributes.");
    }

    @Test
    void testAddAndGetUser() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        User user = new UserBuilder().withId(1L).withUsername("alice").build();
        AuthenticatedUser authUser = new AuthenticatedUser(user, "token123");

        AuthenticatedUserArgumentResolver.addUserTo(authUser, request);
        verify(request).setAttribute("AuthenticatedUserArgumentResolver", authUser);

        when(request.getAttribute("AuthenticatedUserArgumentResolver")).thenReturn(authUser);
        AuthenticatedUser retrievedUser = AuthenticatedUserArgumentResolver.getUserFrom(request);
        assertThat(retrievedUser).isEqualTo(authUser);

        when(request.getAttribute("AuthenticatedUserArgumentResolver")).thenReturn("NotAnAuthenticatedUserObject");
        assertThat(AuthenticatedUserArgumentResolver.getUserFrom(request)).isNull();

        when(request.getAttribute("AuthenticatedUserArgumentResolver")).thenReturn(null);
        assertThat(AuthenticatedUserArgumentResolver.getUserFrom(request)).isNull();
    }

    private static class DummyController {
        public void handleWithAuth(AuthenticatedUser user) {
        }

        public void handleWithoutAuth(String param) {
        }
    }
}