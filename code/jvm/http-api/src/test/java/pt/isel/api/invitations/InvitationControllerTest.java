package pt.isel.api.invitations;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.isel.api.TestConfig;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.common.Either;
import pt.isel.domain.invitations.Invitation;
import pt.isel.domain.users.UserInfo;
import pt.isel.services.invitations.InvitationService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvitationController.class)
@Import(TestConfig.class)
class InvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InvitationService invitationService;

    @Test
    void testCreateInvitation() throws Exception {
        LocalDateTime expiry = LocalDateTime.now().plusDays(1);
        InvitationInput input = new InvitationInput(AccessType.READ_ONLY, expiry);
        Invitation invitation = new Invitation(100L, "token123", new UserInfo(1L, "testuser"), new Channel(10L, "Secret", new UserInfo(1L, "testuser")), AccessType.READ_ONLY, expiry);

        when(invitationService.createInvitation(eq(1L), eq(10L), eq(AccessType.READ_ONLY), any())).thenReturn(Either.success(invitation));

        mockMvc.perform(post("/api/channels/10/invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.token").value("token123"));
    }

    @Test
    void testGetInvitationsForChannel() throws Exception {
        when(invitationService.getInvitationsForChannel(1L, 10L)).thenReturn(Either.success(List.of()));

        mockMvc.perform(get("/api/channels/10/invitations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testRevokeInvitation() throws Exception {
        when(invitationService.revokeInvitation(1L, 10L, 100L)).thenReturn(Either.success("Revoked"));

        mockMvc.perform(post("/api/channels/10/invitations/100/revoke"))
                .andExpect(status().isOk());
    }

    @Test
    void testCreateInvitationValidationFailure() throws Exception {
        String invalidJson = "{}";

        mockMvc.perform(post("/api/channels/10/invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Request Content"));
    }
}