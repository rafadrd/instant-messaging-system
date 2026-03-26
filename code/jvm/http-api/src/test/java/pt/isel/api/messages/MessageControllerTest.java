package pt.isel.api.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.isel.api.TestConfig;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.common.Either;
import pt.isel.domain.messages.Message;
import pt.isel.domain.users.UserInfo;
import pt.isel.services.messages.MessageService;

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

    @Test
    void testCreateMessage() throws Exception {
        MessageRequest request = new MessageRequest("Hello World");
        Message message = new Message(100L, "Hello World", new UserInfo(1L, "testuser"), new Channel(10L, "General", new UserInfo(1L, "testuser")));

        when(messageService.createMessage(anyString(), anyLong(), anyLong())).thenReturn(Either.success(message));

        mockMvc.perform(post("/api/channels/10/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.content").value("Hello World"));
    }

    @Test
    void testCreateMessageValidationFailure() throws Exception {
        MessageRequest request = new MessageRequest(""); // Empty message

        mockMvc.perform(post("/api/channels/10/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetMessages() throws Exception {
        when(messageService.getMessagesInChannel(eq(1L), eq(10L), anyInt(), anyInt())).thenReturn(Either.success(List.of()));

        mockMvc.perform(get("/api/channels/10/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}