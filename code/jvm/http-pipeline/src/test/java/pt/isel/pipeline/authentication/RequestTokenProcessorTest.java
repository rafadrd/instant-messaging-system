package pt.isel.pipeline.authentication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.AuthenticatedUser;
import pt.isel.domain.users.User;
import pt.isel.services.users.UserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestTokenProcessorTest {

    private UserService userService;
    private RequestTokenProcessor processor;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        processor = new RequestTokenProcessor(userService);
    }

    @Test
    void testProcessAuthorizationHeaderValueNullOrBlank() {
        assertNull(processor.processAuthorizationHeaderValue(null));
        assertNull(processor.processAuthorizationHeaderValue(""));
        assertNull(processor.processAuthorizationHeaderValue("   "));
    }

    @Test
    void testProcessAuthorizationHeaderValueMalformed() {
        assertNull(processor.processAuthorizationHeaderValue("Bearer"));
        assertNull(processor.processAuthorizationHeaderValue("Bearer token extra"));
    }

    @Test
    void testProcessAuthorizationHeaderValueInvalidScheme() {
        assertNull(processor.processAuthorizationHeaderValue("Basic dXNlcjpwYXNz"));
        assertNull(processor.processAuthorizationHeaderValue("Token some-token"));
    }

    @Test
    void testProcessAuthorizationHeaderValueValidToken() {
        User user = new User(1L, "alice", new PasswordValidationInfo("hash"));
        when(userService.getUserByToken("valid-token")).thenReturn(user);

        AuthenticatedUser authUser = processor.processAuthorizationHeaderValue("Bearer valid-token");

        assertNotNull(authUser);
        assertEquals(user, authUser.user());
        assertEquals("valid-token", authUser.token());
    }

    @Test
    void testProcessAuthorizationHeaderValueValidTokenCaseInsensitiveScheme() {
        User user = new User(1L, "alice", new PasswordValidationInfo("hash"));
        when(userService.getUserByToken("valid-token")).thenReturn(user);

        AuthenticatedUser authUser = processor.processAuthorizationHeaderValue("bEaReR valid-token");

        assertNotNull(authUser);
        assertEquals(user, authUser.user());
        assertEquals("valid-token", authUser.token());
    }

    @Test
    void testProcessAuthorizationHeaderValueInvalidToken() {
        when(userService.getUserByToken("invalid-token")).thenReturn(null);

        AuthenticatedUser authUser = processor.processAuthorizationHeaderValue("Bearer invalid-token");

        assertNull(authUser);
    }
}