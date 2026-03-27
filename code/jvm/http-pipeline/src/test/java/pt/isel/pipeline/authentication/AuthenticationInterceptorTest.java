package pt.isel.pipeline.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;
import pt.isel.domain.common.Either;
import pt.isel.domain.common.UserError;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.AuthenticatedUser;
import pt.isel.domain.users.User;
import pt.isel.services.users.TicketService;
import pt.isel.services.users.UserService;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationInterceptorTest {

    private RequestTokenProcessor tokenProcessor;
    private TicketService ticketService;
    private UserService userService;
    private AuthenticationInterceptor interceptor;

    private HttpServletRequest request;
    private HttpServletResponse response;

    private HandlerMethod authHandlerMethod;
    private HandlerMethod noAuthHandlerMethod;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        tokenProcessor = mock(RequestTokenProcessor.class);
        ticketService = mock(TicketService.class);
        userService = mock(UserService.class);
        interceptor = new AuthenticationInterceptor(tokenProcessor, ticketService, userService);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        Method authMethod = DummyController.class.getMethod("requiresAuth", AuthenticatedUser.class);
        authHandlerMethod = new HandlerMethod(new DummyController(), authMethod);

        Method noAuthMethod = DummyController.class.getMethod("noAuth");
        noAuthHandlerMethod = new HandlerMethod(new DummyController(), noAuthMethod);
    }

    @Test
    void testPreHandleNotHandlerMethod() {
        Object nonHandlerMethod = new Object();
        assertThat(interceptor.preHandle(request, response, nonHandlerMethod)).isTrue();
    }

    @Test
    void testPreHandleNoAuthRequired() {
        assertThat(interceptor.preHandle(request, response, noAuthHandlerMethod)).isTrue();
        verify(request, never()).getHeader(AuthenticationInterceptor.NAME_AUTHORIZATION_HEADER);
    }

    @Test
    void testPreHandleAuthRequiredValidToken() {
        User user = new User(1L, "alice", new PasswordValidationInfo("hash"));
        AuthenticatedUser authUser = new AuthenticatedUser(user, "valid-token");

        when(request.getHeader(AuthenticationInterceptor.NAME_AUTHORIZATION_HEADER)).thenReturn("Bearer valid-token");
        when(tokenProcessor.processAuthorizationHeaderValue("Bearer valid-token")).thenReturn(authUser);

        assertThat(interceptor.preHandle(request, response, authHandlerMethod)).isTrue();
        verify(request).setAttribute("AuthenticatedUserArgumentResolver", authUser);
    }

    @Test
    void testPreHandleAuthRequiredMissingToken() {
        when(request.getHeader(AuthenticationInterceptor.NAME_AUTHORIZATION_HEADER)).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/users/me");

        assertThat(interceptor.preHandle(request, response, authHandlerMethod)).isFalse();
        verify(response).setStatus(401);
        verify(response).addHeader("WWW-Authenticate", "bearer");
    }

    @Test
    void testPreHandleAuthRequiredInvalidToken() {
        when(request.getHeader(AuthenticationInterceptor.NAME_AUTHORIZATION_HEADER)).thenReturn("Bearer invalid-token");
        when(tokenProcessor.processAuthorizationHeaderValue("Bearer invalid-token")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/users/me");

        assertThat(interceptor.preHandle(request, response, authHandlerMethod)).isFalse();
        verify(response).setStatus(401);
        verify(response).addHeader("WWW-Authenticate", "bearer");
    }

    @Test
    void testPreHandleAuthRequiredValidTicketForListenEndpoint() {
        User user = new User(1L, "alice", new PasswordValidationInfo("hash"));

        when(request.getHeader(AuthenticationInterceptor.NAME_AUTHORIZATION_HEADER)).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/channels/10/listen");
        when(request.getParameter("ticket")).thenReturn("valid-ticket");
        when(ticketService.validateAndConsumeTicket("valid-ticket")).thenReturn(1L);
        when(userService.getUserById(1L)).thenReturn(Either.success(user));

        assertThat(interceptor.preHandle(request, response, authHandlerMethod)).isTrue();
        verify(request).setAttribute(
                org.mockito.ArgumentMatchers.eq("AuthenticatedUserArgumentResolver"),
                org.mockito.ArgumentMatchers.any(AuthenticatedUser.class)
        );
    }

    @Test
    void testPreHandleAuthRequiredInvalidTicketForListenEndpoint() {
        when(request.getHeader(AuthenticationInterceptor.NAME_AUTHORIZATION_HEADER)).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/channels/10/listen");
        when(request.getParameter("ticket")).thenReturn("invalid-ticket");
        when(ticketService.validateAndConsumeTicket("invalid-ticket")).thenReturn(null);

        assertThat(interceptor.preHandle(request, response, authHandlerMethod)).isFalse();
        verify(response).setStatus(401);
    }

    @Test
    void testPreHandleAuthRequiredTicketValidButUserNotFound() {
        when(request.getHeader(AuthenticationInterceptor.NAME_AUTHORIZATION_HEADER)).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/channels/10/listen");
        when(request.getParameter("ticket")).thenReturn("valid-ticket");
        when(ticketService.validateAndConsumeTicket("valid-ticket")).thenReturn(999L);
        when(userService.getUserById(999L)).thenReturn(Either.failure(new UserError.UserNotFound()));

        assertThat(interceptor.preHandle(request, response, authHandlerMethod)).isFalse();
        verify(response).setStatus(401);
    }

    @Test
    void testPreHandleAuthRequiredListenEndpointMissingTicket() {
        when(request.getHeader(AuthenticationInterceptor.NAME_AUTHORIZATION_HEADER)).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/channels/10/listen");

        when(request.getParameter("ticket")).thenReturn(null);

        assertThat(interceptor.preHandle(request, response, authHandlerMethod)).isFalse();
        verify(response).setStatus(401);
        verify(response).addHeader("WWW-Authenticate", "bearer");
    }

    private static class DummyController {
        public void requiresAuth(AuthenticatedUser user) {
        }

        public void noAuth() {
        }
    }
}