package pt.isel.pipeline.authentication;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.AuthenticatedUser;
import pt.isel.domain.users.User;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        assertTrue(resolver.supportsParameter(validParameter));
        assertFalse(resolver.supportsParameter(invalidParameter));
    }

    @Test
    void testResolveArgumentSuccess() {
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        ModelAndViewContainer mavContainer = mock(ModelAndViewContainer.class);

        User user = new User(1L, "alice", new PasswordValidationInfo("hash"));
        AuthenticatedUser authUser = new AuthenticatedUser(user, "token123");

        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(request);
        when(request.getAttribute("AuthenticatedUserArgumentResolver")).thenReturn(authUser);

        Object result = resolver.resolveArgument(validParameter, mavContainer, webRequest, null);

        assertEquals(authUser, result);
    }

    @Test
    void testResolveArgumentThrowsWhenRequestIsNull() {
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        ModelAndViewContainer mavContainer = mock(ModelAndViewContainer.class);

        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(null);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                resolver.resolveArgument(validParameter, mavContainer, webRequest, null)
        );

        assertEquals("Failed to extract HttpServletRequest from NativeWebRequest.", exception.getMessage());
    }

    @Test
    void testResolveArgumentThrowsWhenUserNotFoundInAttributes() {
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        ModelAndViewContainer mavContainer = mock(ModelAndViewContainer.class);

        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(request);
        when(request.getAttribute("AuthenticatedUserArgumentResolver")).thenReturn(null);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                resolver.resolveArgument(validParameter, mavContainer, webRequest, null)
        );

        assertEquals("AuthenticatedUser not found in request attributes.", exception.getMessage());
    }

    @Test
    void testAddAndGetUser() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        User user = new User(1L, "alice", new PasswordValidationInfo("hash"));
        AuthenticatedUser authUser = new AuthenticatedUser(user, "token123");

        AuthenticatedUserArgumentResolver.addUserTo(authUser, request);
        verify(request).setAttribute("AuthenticatedUserArgumentResolver", authUser);

        when(request.getAttribute("AuthenticatedUserArgumentResolver")).thenReturn(authUser);
        AuthenticatedUser retrievedUser = AuthenticatedUserArgumentResolver.getUserFrom(request);
        assertEquals(authUser, retrievedUser);

        when(request.getAttribute("AuthenticatedUserArgumentResolver")).thenReturn("NotAnAuthenticatedUserObject");
        assertNull(AuthenticatedUserArgumentResolver.getUserFrom(request));

        when(request.getAttribute("AuthenticatedUserArgumentResolver")).thenReturn(null);
        assertNull(AuthenticatedUserArgumentResolver.getUserFrom(request));
    }

    private static class DummyController {
        public void handleWithAuth(AuthenticatedUser user) {
        }

        public void handleWithoutAuth(String param) {
        }
    }
}