package pt.isel.pipeline.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;
import pt.isel.domain.builders.UserBuilder;
import pt.isel.domain.common.Either;
import pt.isel.domain.common.UserError;
import pt.isel.domain.users.AuthenticatedUser;
import pt.isel.domain.users.User;
import pt.isel.services.users.TicketService;
import pt.isel.services.users.UserService;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationInterceptorTest {

    private static final String VALID_TOKEN = "valid-token";
    private static final String BEARER_VALID_TOKEN = "Bearer " + VALID_TOKEN;
    private static final String INVALID_TOKEN = "invalid-token";
    private static final String BEARER_INVALID_TOKEN = "Bearer " + INVALID_TOKEN;

    @Mock
    private RequestTokenProcessor tokenProcessor;

    @Mock
    private TicketService ticketService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthenticationInterceptor interceptor;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private HandlerMethod authHandlerMethod;
    private HandlerMethod noAuthHandlerMethod;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        Method authMethod = DummyController.class.getMethod("requiresAuth", AuthenticatedUser.class);
        authHandlerMethod = new HandlerMethod(new DummyController(), authMethod);

        Method noAuthMethod = DummyController.class.getMethod("noAuth");
        noAuthHandlerMethod = new HandlerMethod(new DummyController(), noAuthMethod);
    }

    @Test
    void PreHandle_NotHandlerMethod_ReturnsTrue() {
        Object nonHandlerMethod = new Object();
        assertThat(interceptor.preHandle(request, response, nonHandlerMethod)).isTrue();
    }

    @Test
    void PreHandle_NoAuthRequired_ReturnsTrue() {
        assertThat(interceptor.preHandle(request, response, noAuthHandlerMethod)).isTrue();
        verify(request, never()).getHeader(AuthenticationInterceptor.NAME_AUTHORIZATION_HEADER);
    }

    @Test
    void PreHandle_ValidToken_ReturnsTrue() {
        User user = new UserBuilder().withId(1L).withUsername("alice").build();
        AuthenticatedUser authUser = new AuthenticatedUser(user, VALID_TOKEN);

        when(request.getHeader(AuthenticationInterceptor.NAME_AUTHORIZATION_HEADER)).thenReturn(BEARER_VALID_TOKEN);
        when(tokenProcessor.processAuthorizationHeaderValue(BEARER_VALID_TOKEN)).thenReturn(authUser);

        assertThat(interceptor.preHandle(request, response, authHandlerMethod)).isTrue();
        verify(request).setAttribute("AuthenticatedUserArgumentResolver", authUser);
    }

    @Test
    void PreHandle_MissingToken_ReturnsFalse() {
        when(request.getHeader(AuthenticationInterceptor.NAME_AUTHORIZATION_HEADER)).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/users/me");

        assertThat(interceptor.preHandle(request, response, authHandlerMethod)).isFalse();
        verify(response).setStatus(401);
        verify(response).addHeader("WWW-Authenticate", "bearer");
    }

    @Test
    void PreHandle_InvalidToken_ReturnsFalse() {
        when(request.getHeader(AuthenticationInterceptor.NAME_AUTHORIZATION_HEADER)).thenReturn(BEARER_INVALID_TOKEN);
        when(tokenProcessor.processAuthorizationHeaderValue(BEARER_INVALID_TOKEN)).thenReturn(null);

        assertThat(interceptor.preHandle(request, response, authHandlerMethod)).isFalse();
        verify(response).setStatus(401);
        verify(response).addHeader("WWW-Authenticate", "bearer");
    }

    @Test
    void PreHandle_ValidTicket_ReturnsTrue() {
        User user = new UserBuilder().withId(1L).withUsername("alice").build();

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
    void PreHandle_InvalidTicket_ReturnsFalse() {
        when(request.getHeader(AuthenticationInterceptor.NAME_AUTHORIZATION_HEADER)).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/channels/10/listen");
        when(request.getParameter("ticket")).thenReturn("invalid-ticket");
        when(ticketService.validateAndConsumeTicket("invalid-ticket")).thenReturn(null);

        assertThat(interceptor.preHandle(request, response, authHandlerMethod)).isFalse();
        verify(response).setStatus(401);
    }

    @Test
    void PreHandle_UserNotFound_ReturnsFalse() {
        when(request.getHeader(AuthenticationInterceptor.NAME_AUTHORIZATION_HEADER)).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/channels/10/listen");
        when(request.getParameter("ticket")).thenReturn("valid-ticket");
        when(ticketService.validateAndConsumeTicket("valid-ticket")).thenReturn(999L);
        when(userService.getUserById(999L)).thenReturn(Either.failure(new UserError.UserNotFound()));

        assertThat(interceptor.preHandle(request, response, authHandlerMethod)).isFalse();
        verify(response).setStatus(401);
    }

    @Test
    void PreHandle_MissingTicket_ReturnsFalse() {
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