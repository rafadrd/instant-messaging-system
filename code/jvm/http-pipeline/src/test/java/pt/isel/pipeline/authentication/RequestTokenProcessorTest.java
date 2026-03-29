package pt.isel.pipeline.authentication;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.isel.domain.builders.UserBuilder;
import pt.isel.domain.users.AuthenticatedUser;
import pt.isel.domain.users.User;
import pt.isel.services.users.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestTokenProcessorTest {

    private static final String VALID_TOKEN = "valid-token";
    private static final String BEARER_VALID_TOKEN = "Bearer " + VALID_TOKEN;

    @Mock
    private UserService userService;

    @InjectMocks
    private RequestTokenProcessor processor;

    @Test
    void ProcessAuthorizationHeaderValue_NullOrBlank_ReturnsNull() {
        assertThat(processor.processAuthorizationHeaderValue(null)).isNull();
        assertThat(processor.processAuthorizationHeaderValue("")).isNull();
        assertThat(processor.processAuthorizationHeaderValue("   ")).isNull();
    }

    @Test
    void ProcessAuthorizationHeaderValue_Malformed_ReturnsNull() {
        assertThat(processor.processAuthorizationHeaderValue("Bearer")).isNull();
        assertThat(processor.processAuthorizationHeaderValue("Bearer token extra")).isNull();
    }

    @Test
    void ProcessAuthorizationHeaderValue_InvalidScheme_ReturnsNull() {
        assertThat(processor.processAuthorizationHeaderValue("Basic dXNlcjpwYXNz")).isNull();
        assertThat(processor.processAuthorizationHeaderValue("Token some-token")).isNull();
    }

    @Test
    void ProcessAuthorizationHeaderValue_ValidToken_ReturnsUser() {
        User user = new UserBuilder().withId(1L).withUsername("alice").build();
        when(userService.getUserByToken(VALID_TOKEN)).thenReturn(user);

        AuthenticatedUser authUser = processor.processAuthorizationHeaderValue(BEARER_VALID_TOKEN);

        assertThat(authUser).isNotNull();
        assertThat(authUser.user()).isEqualTo(user);
        assertThat(authUser.token()).isEqualTo(VALID_TOKEN);
    }

    @Test
    void ProcessAuthorizationHeaderValue_CaseInsensitiveScheme_ReturnsUser() {
        User user = new UserBuilder().withId(1L).withUsername("alice").build();
        when(userService.getUserByToken(VALID_TOKEN)).thenReturn(user);

        AuthenticatedUser authUser = processor.processAuthorizationHeaderValue("bEaReR " + VALID_TOKEN);

        assertThat(authUser).isNotNull();
        assertThat(authUser.user()).isEqualTo(user);
        assertThat(authUser.token()).isEqualTo(VALID_TOKEN);
    }

    @Test
    void ProcessAuthorizationHeaderValue_InvalidToken_ReturnsNull() {
        when(userService.getUserByToken("invalid-token")).thenReturn(null);

        AuthenticatedUser authUser = processor.processAuthorizationHeaderValue("Bearer invalid-token");

        assertThat(authUser).isNull();
    }
}