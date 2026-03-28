package pt.isel.api.messages;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import pt.isel.api.AbstractControllerTest;
import pt.isel.api.common.Problem;
import pt.isel.domain.builders.MessageBuilder;
import pt.isel.domain.common.Either;
import pt.isel.domain.messages.Message;
import pt.isel.services.messages.MessageService;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pt.isel.api.common.ProblemResultMatchers.isProblem;

@WebMvcTest(MessageController.class)
class MessageControllerTest extends AbstractControllerTest {

    @MockitoBean
    private MessageService messageService;

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

        postWithAuth("/api/channels/10/messages", request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.content").value("Hello World"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testCreateMessageValidationFailure_Empty(String invalidContent) throws Exception {
        MessageRequest request = new MessageRequest(invalidContent);

        postWithAuth("/api/channels/10/messages", request)
                .andExpect(isProblem(Problem.InvalidRequestContent));
    }

    @Test
    void testCreateMessageValidationFailure_TooLong() throws Exception {
        MessageRequest request = new MessageRequest("a".repeat(1001));

        postWithAuth("/api/channels/10/messages", request)
                .andExpect(isProblem(Problem.InvalidRequestContent));
    }

    @Test
    void testGetMessages() throws Exception {
        when(messageService.getMessagesInChannel(eq(1L), eq(10L), anyInt(), anyInt())).thenReturn(Either.success(List.of()));

        getWithAuth("/api/channels/10/messages")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}