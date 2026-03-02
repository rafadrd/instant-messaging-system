package pt.isel.pipeline.authentication;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import pt.isel.domain.users.AuthenticatedUser;

@Component
public class AuthenticatedUserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String KEY = "AuthenticatedUserArgumentResolver";

    public static void addUserTo(AuthenticatedUser user, HttpServletRequest request) {
        request.setAttribute(KEY, user);
    }

    public static AuthenticatedUser getUserFrom(HttpServletRequest request) {
        Object user = request.getAttribute(KEY);
        return user instanceof AuthenticatedUser ? (AuthenticatedUser) user : null;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == AuthenticatedUser.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new IllegalStateException("Failed to extract HttpServletRequest from NativeWebRequest.");
        }

        AuthenticatedUser user = getUserFrom(request);
        if (user == null) {
            throw new IllegalStateException("AuthenticatedUser not found in request attributes.");
        }
        return user;
    }
}