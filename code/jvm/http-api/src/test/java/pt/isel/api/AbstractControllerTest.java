package pt.isel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pt.isel.domain.builders.UserBuilder;
import pt.isel.domain.users.AuthenticatedUser;
import pt.isel.domain.users.User;
import pt.isel.pipeline.authentication.RequestTokenProcessor;
import pt.isel.services.users.TicketService;
import pt.isel.services.users.UserService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

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

    protected ResultActions getWithAuth(String url) throws Exception {
        return mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN));
    }

    protected ResultActions deleteWithAuth(String url) throws Exception {
        return mockMvc.perform(delete(url).header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN));
    }

    protected ResultActions postWithAuth(String url, Object body) throws Exception {
        return performWithBody(post(url), body, true);
    }

    protected ResultActions postWithAuth(String url) throws Exception {
        return mockMvc.perform(post(url).header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN));
    }

    protected ResultActions putWithAuth(String url, Object body) throws Exception {
        return performWithBody(put(url), body, true);
    }

    protected ResultActions postWithoutAuth(String url, Object body) throws Exception {
        return performWithBody(post(url), body, false);
    }

    private ResultActions performWithBody(MockHttpServletRequestBuilder builder, Object body, boolean withAuth) throws Exception {
        if (withAuth) {
            builder.header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN);
        }
        return mockMvc.perform(builder
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }
}