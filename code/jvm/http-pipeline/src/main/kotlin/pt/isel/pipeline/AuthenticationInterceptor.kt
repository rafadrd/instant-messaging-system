package pt.isel.pipeline

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import pt.isel.domain.auth.AuthenticatedUser

@Component
class AuthenticationInterceptor(
    private val authorizationHeaderProcessor: RequestTokenProcessor,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (
            handler is HandlerMethod &&
            handler.methodParameters.any { it.parameterType == AuthenticatedUser::class.java }
        ) {
            var authorizationValue = request.getHeader(NAME_AUTHORIZATION_HEADER)

            if (authorizationValue == null) {
                val accessToken = request.getParameter("access_token")
                if (accessToken != null) {
                    authorizationValue = "$SCHEME $accessToken"
                }
            }

            val user =
                authorizationValue?.let {
                    authorizationHeaderProcessor.processAuthorizationHeaderValue(it)
                }

            return if (user == null) {
                response.apply {
                    status = 401
                    addHeader(NAME_WWW_AUTHENTICATE_HEADER, RequestTokenProcessor.SCHEME)
                }
                false
            } else {
                AuthenticatedUserArgumentResolver.addUserTo(user, request)
                true
            }
        }

        return true
    }

    companion object {
        const val NAME_AUTHORIZATION_HEADER = "Authorization"
        private const val NAME_WWW_AUTHENTICATE_HEADER = "WWW-Authenticate"
        private const val SCHEME = "bearer"
    }
}
