package pt.isel.pipeline.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import pt.isel.domain.common.Either;
import pt.isel.domain.common.UserError;
import pt.isel.domain.users.AuthenticatedUser;
import pt.isel.domain.users.User;
import pt.isel.services.users.TicketService;
import pt.isel.services.users.UserService;

import java.util.Arrays;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {
    public static final String NAME_AUTHORIZATION_HEADER = "Authorization";
    private static final String NAME_WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";

    private final RequestTokenProcessor tokenProcessor;
    private final TicketService ticketService;
    private final UserService userService;

    public AuthenticationInterceptor(RequestTokenProcessor tokenProcessor, TicketService ticketService, UserService userService) {
        this.tokenProcessor = tokenProcessor;
        this.ticketService = ticketService;
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            boolean requiresAuth = Arrays.stream(handlerMethod.getMethodParameters())
                    .anyMatch(p -> p.getParameterType() == AuthenticatedUser.class);

            if (requiresAuth) {
                AuthenticatedUser user = null;
                String authValue = request.getHeader(NAME_AUTHORIZATION_HEADER);

                if (authValue != null) {
                    user = tokenProcessor.processAuthorizationHeaderValue(authValue);
                }
                else if (request.getRequestURI().endsWith("/listen")) {
                    String ticket = request.getParameter("ticket");
                    if (ticket != null) {
                        Long userId = ticketService.validateAndConsumeTicket(ticket);
                        if (userId != null) {
                            var result = userService.getUserById(userId);
                            if (result instanceof Either.Right<UserError, User>(User value)) {
                                user = new AuthenticatedUser(value, "ticket-session");
                            }
                        }
                    }
                }

                if (user == null) {
                    response.setStatus(401);
                    response.addHeader(NAME_WWW_AUTHENTICATE_HEADER, RequestTokenProcessor.SCHEME);
                    return false;
                }

                AuthenticatedUserArgumentResolver.addUserTo(user, request);
                return true;
            }
        }
        return true;
    }
}