package pt.isel.api.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.isel.api.TestConfig;
import pt.isel.domain.common.Either;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.security.TokenExternalInfo;
import pt.isel.domain.users.User;
import pt.isel.services.channels.ChannelService;
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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ChannelService channelService;

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

    @Test
    void testRegisterUserValidationFailure() throws Exception {
        RegisterInput input = new RegisterInput("", "Strong1!", null); // Empty username

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

    @Test
    void testLogoutUser() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());

        verify(userService).revokeToken("mock-token");
    }

    @Test
    void testUserHome() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void testUpdateUsername() throws Exception {
        UpdateUsernameInput input = new UpdateUsernameInput("new_alice", "Strong1!");
        User updatedUser = new User(1L, "new_alice", new PasswordValidationInfo("hash"));

        when(userService.updateUsername(eq(1L), eq("new_alice"), eq("Strong1!"))).thenReturn(Either.success(updatedUser));

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new_alice"));
    }

    @Test
    void testUpdatePassword() throws Exception {
        UpdatePasswordInput input = new UpdatePasswordInput("Strong1!", "Stronger2@");
        User updatedUser = new User(1L, "testuser", new PasswordValidationInfo("newhash"));

        when(userService.updatePassword(eq(1L), eq("Strong1!"), eq("Stronger2@"))).thenReturn(Either.success(updatedUser));

        mockMvc.perform(put("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk());
    }

    @Test
    void testGetUserChannels() throws Exception {
        when(channelService.getJoinedChannels(eq(1L), eq(50), eq(0))).thenReturn(Either.success(List.of()));

        mockMvc.perform(get("/api/users/me/channels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testDeleteUser() throws Exception {
        when(userService.deleteUser(1L)).thenReturn(Either.success("Deleted"));

        mockMvc.perform(delete("/api/users/me"))
                .andExpect(status().isOk());
    }

    @Test
    void testLoginUserValidationFailure() throws Exception {
        UserInput input = new UserInput("alice", "");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdatePasswordValidationFailure() throws Exception {
        UpdatePasswordInput input = new UpdatePasswordInput("OldPassword1!", "");

        mockMvc.perform(put("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());
    }
}