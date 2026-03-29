package pt.isel.api.users;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import pt.isel.api.AbstractControllerTest;
import pt.isel.api.common.Problem;
import pt.isel.domain.builders.UserBuilder;
import pt.isel.domain.common.Either;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.security.TokenExternalInfo;
import pt.isel.domain.users.User;
import pt.isel.services.channels.ChannelService;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pt.isel.api.common.ProblemResultMatchers.isProblem;

@WebMvcTest(UserController.class)
class UserControllerTest extends AbstractControllerTest {

    @MockitoBean
    private ChannelService channelService;

    @Test
    void GetMe_WithoutAuth_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void RegisterUser_ValidInput_ReturnsCreated() throws Exception {
        RegisterInput input = new RegisterInput("alice", "Strong1!", null);
        TokenExternalInfo tokenInfo = new TokenExternalInfo("token123", Instant.now(), 1L);

        when(userService.registerUser(anyString(), anyString(), any())).thenReturn(Either.success(tokenInfo));

        postWithoutAuth("/api/auth/register", input)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.token").value("token123"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void RegisterUser_InvalidUsername_ReturnsBadRequest(String invalidUsername) throws Exception {
        RegisterInput input = new RegisterInput(invalidUsername, "Strong1!", null);

        postWithoutAuth("/api/auth/register", input)
                .andExpect(isProblem(Problem.InvalidRequestContent));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void RegisterUser_InvalidPassword_ReturnsBadRequest(String invalidPassword) throws Exception {
        RegisterInput input = new RegisterInput("alice", invalidPassword, null);

        postWithoutAuth("/api/auth/register", input)
                .andExpect(isProblem(Problem.InvalidRequestContent));
    }

    @Test
    void LoginUser_ValidInput_ReturnsOk() throws Exception {
        UserInput input = new UserInput("alice", "Strong1!");
        TokenExternalInfo tokenInfo = new TokenExternalInfo("token123", Instant.now(), 1L);

        when(userService.createToken(anyString(), anyString())).thenReturn(Either.success(tokenInfo));

        postWithoutAuth("/api/auth/login", input)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.token").value("token123"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void LoginUser_InvalidInput_ReturnsBadRequest(String invalidInput) throws Exception {
        UserInput input = new UserInput("alice", invalidInput);

        postWithoutAuth("/api/auth/login", input)
                .andExpect(isProblem(Problem.InvalidRequestContent));
    }

    @Test
    void LogoutUser_ValidToken_ReturnsNoContent() throws Exception {
        postWithAuth("/api/auth/logout")
                .andExpect(status().isNoContent());

        verify(userService).revokeToken(MOCK_TOKEN);
    }

    @Test
    void UserHome_ValidToken_ReturnsUserInfo() throws Exception {
        getWithAuth("/api/users/me")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void UpdateUsername_ValidInput_ReturnsUpdatedUser() throws Exception {
        UpdateUsernameInput input = new UpdateUsernameInput("new_alice", "Strong1!");
        User updatedUser = new UserBuilder().withId(1L).withUsername("new_alice").build();

        when(userService.updateUsername(eq(1L), eq("new_alice"), eq("Strong1!"))).thenReturn(Either.success(updatedUser));

        putWithAuth("/api/users/me", input)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new_alice"));
    }

    @Test
    void UpdatePassword_ValidInput_ReturnsOk() throws Exception {
        UpdatePasswordInput input = new UpdatePasswordInput("Strong1!", "Stronger2@");
        User updatedUser = new UserBuilder().withId(1L).withPasswordValidation(new PasswordValidationInfo("newhash")).build();

        when(userService.updatePassword(eq(1L), eq("Strong1!"), eq("Stronger2@"))).thenReturn(Either.success(updatedUser));

        putWithAuth("/api/users/me/password", input)
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void UpdatePassword_InvalidInput_ReturnsBadRequest(String invalidPassword) throws Exception {
        UpdatePasswordInput input = new UpdatePasswordInput("OldPassword1!", invalidPassword);

        putWithAuth("/api/users/me/password", input)
                .andExpect(isProblem(Problem.InvalidRequestContent));
    }

    @Test
    void GetUserChannels_ValidToken_ReturnsChannels() throws Exception {
        when(channelService.getJoinedChannels(eq(1L), eq(50), eq(0))).thenReturn(Either.success(List.of()));

        getWithAuth("/api/users/me/channels")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void DeleteUser_ValidToken_ReturnsOk() throws Exception {
        when(userService.deleteUser(1L)).thenReturn(Either.success("Deleted"));

        deleteWithAuth("/api/users/me")
                .andExpect(status().isOk());
    }
}