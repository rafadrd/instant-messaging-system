package pt.isel.pipeline.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import pt.isel.domain.users.AuthenticatedUser;

import java.util.Arrays;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {
    public static final String NAME_AUTHORIZATION_HEADER = "Authorization";
    private static final String NAME_WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
    private final RequestTokenProcessor tokenProcessor;

    public AuthenticationInterceptor(RequestTokenProcessor tokenProcessor) {
        this.tokenProcessor = tokenProcessor;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            boolean requiresAuth = Arrays.stream(handlerMethod.getMethodParameters())
                    .anyMatch(p -> p.getParameterType() == AuthenticatedUser.class);

            if (requiresAuth) {
                String authValue = request.getHeader(NAME_AUTHORIZATION_HEADER);

                if (authValue == null && request.getRequestURI().endsWith("/listen")) {
                    String accessToken = request.getParameter("access_token");
                    if (accessToken != null) {
                        authValue = RequestTokenProcessor.SCHEME + " " + accessToken;
                    }
                }

                AuthenticatedUser user = authValue != null
                        ? tokenProcessor.processAuthorizationHeaderValue(authValue)
                        : null;

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