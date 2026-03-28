package pt.isel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.isel.domain.builders.UserBuilder;
import pt.isel.domain.users.AuthenticatedUser;
import pt.isel.domain.users.User;
import pt.isel.pipeline.authentication.RequestTokenProcessor;
import pt.isel.services.users.TicketService;
import pt.isel.services.users.UserService;

import static org.mockito.Mockito.when;

@Import(TestConfig.class)
public abstract class AbstractControllerTest {

    protected static final String MOCK_TOKEN = "mock-token";
    protected static final String BEARER_TOKEN = "Bearer " + MOCK_TOKEN;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected RequestTokenProcessor requestTokenProcessor;

    @MockitoBean
    protected TicketService ticketService;

    @MockitoBean
    protected UserService userService;

    @BeforeEach
    void setUpAuth() {
        User mockUser = new UserBuilder().withId(1L).withUsername("testuser").build();
        AuthenticatedUser authUser = new AuthenticatedUser(mockUser, MOCK_TOKEN);
        when(requestTokenProcessor.processAuthorizationHeaderValue(BEARER_TOKEN)).thenReturn(authUser);
    }
}