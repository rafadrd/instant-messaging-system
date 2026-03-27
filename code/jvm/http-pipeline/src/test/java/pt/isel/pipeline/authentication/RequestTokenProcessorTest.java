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
        when(userService.getUserByToken("valid-token")).thenReturn(user);

        AuthenticatedUser authUser = processor.processAuthorizationHeaderValue("Bearer valid-token");

        assertThat(authUser).isNotNull();
        assertThat(authUser.user()).isEqualTo(user);
        assertThat(authUser.token()).isEqualTo("valid-token");
    }

    @Test
    void testProcessAuthorizationHeaderValueValidTokenCaseInsensitiveScheme() {
        User user = new UserBuilder().withId(1L).withUsername("alice").build();
        when(userService.getUserByToken("valid-token")).thenReturn(user);

        AuthenticatedUser authUser = processor.processAuthorizationHeaderValue("bEaReR valid-token");

        assertThat(authUser).isNotNull();
        assertThat(authUser.user()).isEqualTo(user);
        assertThat(authUser.token()).isEqualTo("valid-token");
    }

    @Test
    void testProcessAuthorizationHeaderValueInvalidToken() {
        when(userService.getUserByToken("invalid-token")).thenReturn(null);

        AuthenticatedUser authUser = processor.processAuthorizationHeaderValue("Bearer invalid-token");

        assertThat(authUser).isNull();
    }
}