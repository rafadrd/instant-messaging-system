package pt.isel.api.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.isel.api.TestConfig;
import pt.isel.domain.builders.UserBuilder;
import pt.isel.domain.common.Either;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.security.TokenExternalInfo;
import pt.isel.domain.users.AuthenticatedUser;
import pt.isel.domain.users.User;
import pt.isel.pipeline.authentication.RequestTokenProcessor;
import pt.isel.services.channels.ChannelService;
import pt.isel.services.users.TicketService;
import pt.isel.services.users.UserService;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(TestConfig.class)
class UserControllerTest {

    private static final String MOCK_TOKEN = "mock-token";
    private static final String BEARER_TOKEN = "Bearer " + MOCK_TOKEN;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ChannelService channelService;

    @MockitoBean
    private RequestTokenProcessor requestTokenProcessor;

    @MockitoBean
    private TicketService ticketService;

    @BeforeEach
    void setUpAuth() {
        User mockUser = new UserBuilder().withId(1L).withUsername("testuser").build();
        AuthenticatedUser authUser = new AuthenticatedUser(mockUser, MOCK_TOKEN);
        when(requestTokenProcessor.processAuthorizationHeaderValue(BEARER_TOKEN)).thenReturn(authUser);
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRegisterUserSuccess() throws Exception {
        RegisterInput input = new RegisterInput("alice", "Strong1!", null);
        TokenExternalInfo tokenInfo = new TokenExternalInfo("token123", Instant.now(), 1L);

        when(userService.registerUser(anyString(), anyString(), any())).thenReturn(Either.success(tokenInfo));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.token").value("token123"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testRegisterUserValidationFailure_InvalidUsername(String invalidUsername) throws Exception {
        RegisterInput input = new RegisterInput(invalidUsername, "Strong1!", null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testRegisterUserValidationFailure_InvalidPassword(String invalidPassword) throws Exception {
        RegisterInput input = new RegisterInput("alice", invalidPassword, null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLoginUserSuccess() throws Exception {
        UserInput input = new UserInput("alice", "Strong1!");
        TokenExternalInfo tokenInfo = new TokenExternalInfo("token123", Instant.now(), 1L);

        when(userService.createToken(anyString(), anyString())).thenReturn(Either.success(tokenInfo));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.token").value("token123"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testLoginUserValidationFailure(String invalidInput) throws Exception {
        UserInput input = new UserInput("alice", invalidInput);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLogoutUser() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN))
                .andExpect(status().isNoContent());

        verify(userService).revokeToken(MOCK_TOKEN);
    }

    @Test
    void testUserHome() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void testUpdateUsername() throws Exception {
        UpdateUsernameInput input = new UpdateUsernameInput("new_alice", "Strong1!");
        User updatedUser = new UserBuilder().withId(1L).withUsername("new_alice").build();

        when(userService.updateUsername(eq(1L), eq("new_alice"), eq("Strong1!"))).thenReturn(Either.success(updatedUser));

        mockMvc.perform(put("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new_alice"));
    }

    @Test
    void testUpdatePassword() throws Exception {
        UpdatePasswordInput input = new UpdatePasswordInput("Strong1!", "Stronger2@");
        User updatedUser = new UserBuilder().withId(1L).withPasswordValidation(new PasswordValidationInfo("newhash")).build();

        when(userService.updatePassword(eq(1L), eq("Strong1!"), eq("Stronger2@"))).thenReturn(Either.success(updatedUser));

        mockMvc.perform(put("/api/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testUpdatePasswordValidationFailure(String invalidPassword) throws Exception {
        UpdatePasswordInput input = new UpdatePasswordInput("OldPassword1!", invalidPassword);

        mockMvc.perform(put("/api/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetUserChannels() throws Exception {
        when(channelService.getJoinedChannels(eq(1L), eq(50), eq(0))).thenReturn(Either.success(List.of()));

        mockMvc.perform(get("/api/users/me/channels")
                        .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testDeleteUser() throws Exception {
        when(userService.deleteUser(1L)).thenReturn(Either.success("Deleted"));

        mockMvc.perform(delete("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN))
                .andExpect(status().isOk());
    }
}