package pt.isel.api.invitations;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import pt.isel.api.AbstractControllerTest;
import pt.isel.api.common.Problem;
import pt.isel.domain.builders.InvitationBuilder;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.common.Either;
import pt.isel.domain.invitations.Invitation;
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
import static pt.isel.api.common.ProblemResultMatchers.isProblem;

@WebMvcTest(InvitationController.class)
class InvitationControllerTest extends AbstractControllerTest {

    @MockitoBean
    private InvitationService invitationService;

    @Test
    void GetInvitations_WithoutAuth_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/channels/10/invitations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void CreateInvitation_ValidInput_ReturnsCreated() throws Exception {
        LocalDateTime expiry = LocalDateTime.now().plusDays(1);
        InvitationInput input = new InvitationInput(AccessType.READ_ONLY, expiry);
        Invitation invitation = new InvitationBuilder()
                .withId(100L)
                .withToken("token123")
                .withAccessType(AccessType.READ_ONLY)
                .withExpiresAt(expiry)
                .build();

        when(invitationService.createInvitation(eq(1L), eq(10L), eq(AccessType.READ_ONLY), any())).thenReturn(Either.success(invitation));

        postWithAuth("/api/channels/10/invitations", input)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.token").value("token123"));
    }

    @Test
    void GetInvitationsForChannel_ValidId_ReturnsInvitations() throws Exception {
        when(invitationService.getInvitationsForChannel(1L, 10L)).thenReturn(Either.success(List.of()));

        getWithAuth("/api/channels/10/invitations")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void RevokeInvitation_ValidId_ReturnsOk() throws Exception {
        when(invitationService.revokeInvitation(1L, 10L, 100L)).thenReturn(Either.success("Revoked"));

        postWithAuth("/api/channels/10/invitations/100/revoke")
                .andExpect(status().isOk());
    }

    @Test
    void CreateInvitation_InvalidInput_ReturnsBadRequest() throws Exception {
        String invalidJson = "{}";

        mockMvc.perform(post("/api/channels/10/invitations")
                        .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(isProblem(Problem.InvalidRequestContent));
    }
}