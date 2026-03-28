package pt.isel.pipeline.authentication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.isel.domain.builders.UserBuilder;
import pt.isel.domain.users.AuthenticatedUser;
import pt.isel.domain.users.User;
import pt.isel.services.users.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestTokenProcessorTest {

    private static final String VALID_TOKEN = "valid-token";
    private static final String BEARER_VALID_TOKEN = "Bearer " + VALID_TOKEN;

    private UserService userService;
    private RequestTokenProcessor processor;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        processor = new RequestTokenProcessor(userService);
    }

    @Test
    void testProcessAuthorizationHeaderValueNullOrBlank() {
        assertThat(processor.processAuthorizationHeaderValue(null)).isNull();
        assertThat(processor.processAuthorizationHeaderValue("")).isNull();
        assertThat(processor.processAuthorizationHeaderValue("   ")).isNull();
    }

    @Test
    void testProcessAuthorizationHeaderValueMalformed() {
        assertThat(processor.processAuthorizationHeaderValue("Bearer")).isNull();
        assertThat(processor.processAuthorizationHeaderValue("Bearer token extra")).isNull();
    }

    @Test
    void testProcessAuthorizationHeaderValueInvalidScheme() {
        assertThat(processor.processAuthorizationHeaderValue("Basic dXNlcjpwYXNz")).isNull();
        assertThat(processor.processAuthorizationHeaderValue("Token some-token")).isNull();
    }

    @Test
    void testProcessAuthorizationHeaderValueValidToken() {
        User user = new UserBuilder().withId(1L).withUsername("alice").build();
        when(userService.getUserByToken(VALID_TOKEN)).thenReturn(user);

        AuthenticatedUser authUser = processor.processAuthorizationHeaderValue(BEARER_VALID_TOKEN);

        assertThat(authUser).isNotNull();
        assertThat(authUser.user()).isEqualTo(user);
        assertThat(authUser.token()).isEqualTo(VALID_TOKEN);
    }

    @Test
    void testProcessAuthorizationHeaderValueValidTokenCaseInsensitiveScheme() {
        User user = new UserBuilder().withId(1L).withUsername("alice").build();
        when(userService.getUserByToken(VALID_TOKEN)).thenReturn(user);

        AuthenticatedUser authUser = processor.processAuthorizationHeaderValue("bEaReR " + VALID_TOKEN);

        assertThat(authUser).isNotNull();
        assertThat(authUser.user()).isEqualTo(user);
        assertThat(authUser.token()).isEqualTo(VALID_TOKEN);
    }

    @Test
    void testProcessAuthorizationHeaderValueInvalidToken() {
        when(userService.getUserByToken("invalid-token")).thenReturn(null);

        AuthenticatedUser authUser = processor.processAuthorizationHeaderValue("Bearer invalid-token");

        assertThat(authUser).isNull();
    }
}