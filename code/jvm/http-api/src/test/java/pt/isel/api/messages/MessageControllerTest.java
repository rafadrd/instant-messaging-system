package pt.isel.api.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.isel.api.TestConfig;
import pt.isel.domain.builders.MessageBuilder;
import pt.isel.domain.builders.UserBuilder;
import pt.isel.domain.common.Either;
import pt.isel.domain.messages.Message;
import pt.isel.domain.users.AuthenticatedUser;
import pt.isel.domain.users.User;
import pt.isel.pipeline.authentication.RequestTokenProcessor;
import pt.isel.services.messages.MessageService;
import pt.isel.services.users.TicketService;
import pt.isel.services.users.UserService;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MessageController.class)
@Import(TestConfig.class)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MessageService messageService;

    @MockitoBean
    private RequestTokenProcessor requestTokenProcessor;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private UserService userService;

    @BeforeEach
    void setUpAuth() {
        User mockUser = new UserBuilder().withId(1L).withUsername("testuser").build();
        AuthenticatedUser authUser = new AuthenticatedUser(mockUser, "mock-token");
        when(requestTokenProcessor.processAuthorizationHeaderValue("Bearer mock-token")).thenReturn(authUser);
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/channels/10/messages"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCreateMessage() throws Exception {
        MessageRequest request = new MessageRequest("Hello World");
        Message message = new MessageBuilder().withId(100L).withContent("Hello World").build();

        when(messageService.createMessage(anyString(), anyLong(), anyLong())).thenReturn(Either.success(message));

        mockMvc.perform(post("/api/channels/10/messages")
                        .header("Authorization", "Bearer mock-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.content").value("Hello World"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testCreateMessageValidationFailure_Empty(String invalidContent) throws Exception {
        MessageRequest request = new MessageRequest(invalidContent);

        mockMvc.perform(post("/api/channels/10/messages")
                        .header("Authorization", "Bearer mock-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateMessageValidationFailure_TooLong() throws Exception {
        MessageRequest request = new MessageRequest("a".repeat(1001));

        mockMvc.perform(post("/api/channels/10/messages")
                        .header("Authorization", "Bearer mock-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetMessages() throws Exception {
        when(messageService.getMessagesInChannel(eq(1L), eq(10L), anyInt(), anyInt())).thenReturn(Either.success(List.of()));

        mockMvc.perform(get("/api/channels/10/messages")
                        .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}