package pt.isel.api.common;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import pt.isel.api.AbstractControllerTest;
import pt.isel.api.users.UserController;
import pt.isel.services.channels.ChannelService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static pt.isel.api.common.ProblemResultMatchers.isProblem;

@WebMvcTest(UserController.class)
class CustomExceptionHandlerTest extends AbstractControllerTest {

    @MockitoBean
    private ChannelService channelService;

    @Test
    void testHandleAllExceptions() throws Exception {
        when(userService.registerUser(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Unexpected database failure"));

        String validJson = "{\"username\":\"alice\", \"password\":\"Strong1!\"}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(isProblem(Problem.InternalServerError));
    }

    @Test
    void testHandleHttpMessageNotReadable() throws Exception {
        String malformedJson = "{\"username\":\"alice\", \"password\":\"Strong1!";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(isProblem(Problem.InvalidRequestContent));
    }

    @Test
    void testHandleMethodArgumentNotValid() throws Exception {
        String invalidJson = "{\"username\":\"\", \"password\":\"Strong1!\"}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(isProblem(Problem.InvalidRequestContent));
    }
}