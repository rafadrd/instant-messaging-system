package pt.isel.api.channels;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import pt.isel.api.AbstractControllerTest;
import pt.isel.api.common.Problem;
import pt.isel.domain.builders.ChannelBuilder;
import pt.isel.domain.builders.ChannelMemberBuilder;
import pt.isel.domain.builders.UserInfoBuilder;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.channels.ChannelMember;
import pt.isel.domain.common.Either;
import pt.isel.services.channels.ChannelService;
import pt.isel.services.messages.MessageEventService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pt.isel.api.common.ProblemResultMatchers.isProblem;

@WebMvcTest(ChannelController.class)
class ChannelControllerTest extends AbstractControllerTest {

    @MockitoBean
    private ChannelService channelService;

    @MockitoBean
    private MessageEventService messageEventService;

    @Test
    void GetChannel_WithoutAuth_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/channels/10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void GetChannels_ValidRequest_ReturnsChannels() throws Exception {
        when(channelService.searchChannels(anyString(), anyInt(), anyInt())).thenReturn(Either.success(List.of()));

        getWithAuth("/api/channels")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void CreateChannel_ValidInput_ReturnsCreated() throws Exception {
        ChannelInput input = new ChannelInput("General", true);
        Channel channel = new ChannelBuilder().withId(10L).withName("General").build();

        when(channelService.createChannel(anyString(), anyLong(), anyBoolean())).thenReturn(Either.success(channel));

        postWithAuth("/api/channels", input)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("General"));
    }

    @Test
    void GetChannelById_ValidId_ReturnsChannel() throws Exception {
        Channel channel = new ChannelBuilder().withId(10L).build();
        when(channelService.getChannelById(10L)).thenReturn(Either.success(channel));

        getWithAuth("/api/channels/10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void EditChannel_ValidInput_ReturnsUpdatedChannel() throws Exception {
        EditChannelInput input = new EditChannelInput("NewName", false);
        Channel channel = new ChannelBuilder().withId(10L).withName("NewName").withIsPublic(false).build();

        when(channelService.editChannel(eq(1L), eq(10L), eq("NewName"), eq(false))).thenReturn(Either.success(channel));

        putWithAuth("/api/channels/10", input)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("NewName"));
    }

    @Test
    void DeleteChannel_ValidId_ReturnsOk() throws Exception {
        when(channelService.deleteChannel(1L, 10L)).thenReturn(Either.success("Deleted"));

        deleteWithAuth("/api/channels/10")
                .andExpect(status().isOk());
    }

    @Test
    void JoinChannel_ValidId_ReturnsOk() throws Exception {
        when(channelService.joinPublicChannel(1L, 10L)).thenReturn(Either.success("Joined"));

        postWithAuth("/api/channels/10/join")
                .andExpect(status().isOk());
    }

    @Test
    void JoinChannelByToken_ValidToken_ReturnsOk() throws Exception {
        JoinByTokenInput input = new JoinByTokenInput("token123");
        when(channelService.joinPrivateChannel(1L, "token123")).thenReturn(Either.success("Joined"));

        postWithAuth("/api/channels/join-by-token", input)
                .andExpect(status().isOk());
    }

    @Test
    void LeaveChannel_ValidId_ReturnsOk() throws Exception {
        when(channelService.leaveChannel(10L, 1L)).thenReturn(Either.success("Left"));

        postWithAuth("/api/channels/10/leave")
                .andExpect(status().isOk());
    }

    @Test
    void GetMembers_ValidId_ReturnsMembers() throws Exception {
        when(channelService.getUsersInChannel(eq(10L), anyInt(), anyInt())).thenReturn(Either.success(List.of()));

        getWithAuth("/api/channels/10/members")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void GetAccessType_ValidIds_ReturnsAccessType() throws Exception {
        when(channelService.getAccessType(1L, 2L, 10L)).thenReturn(Either.success(AccessType.READ_ONLY));

        getWithAuth("/api/channels/10/members/2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("READ_ONLY"));
    }

    @Test
    void EditMemberAccess_ValidInput_ReturnsUpdatedMember() throws Exception {
        EditMemberInput input = new EditMemberInput(AccessType.READ_WRITE);
        ChannelMember member = new ChannelMemberBuilder()
                .withId(1L)
                .withUser(new UserInfoBuilder().withId(2L).withUsername("bob").build())
                .withAccessType(AccessType.READ_WRITE)
                .build();

        when(channelService.editMemberAccess(1L, 10L, 2L, AccessType.READ_WRITE)).thenReturn(Either.success(member));

        putWithAuth("/api/channels/10/members/2", input)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessType").value("READ_WRITE"));
    }

    @Test
    void GetSocketTicket_ValidId_ReturnsTicket() throws Exception {
        when(channelService.getAccessType(1L, 1L, 10L)).thenReturn(Either.success(AccessType.READ_WRITE));
        when(ticketService.createTicket(1L)).thenReturn("ticket-uuid");

        postWithAuth("/api/channels/10/socket-ticket")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket").value("ticket-uuid"));
    }

    @Test
    void Listen_ValidId_ReturnsSseEmitter() throws Exception {
        when(channelService.getAccessType(1L, 1L, 10L)).thenReturn(Either.success(AccessType.READ_WRITE));

        getWithAuth("/api/channels/10/listen")
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void CreateChannel_EmptyName_ReturnsBadRequest(String invalidName) throws Exception {
        ChannelInput input = new ChannelInput(invalidName, true);

        postWithAuth("/api/channels", input)
                .andExpect(isProblem(Problem.InvalidRequestContent));
    }

    @Test
    void CreateChannel_NameTooLong_ReturnsBadRequest() throws Exception {
        ChannelInput input = new ChannelInput("a".repeat(31), true);

        postWithAuth("/api/channels", input)
                .andExpect(isProblem(Problem.InvalidRequestContent));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void JoinChannelByToken_EmptyToken_ReturnsBadRequest(String invalidToken) throws Exception {
        JoinByTokenInput input = new JoinByTokenInput(invalidToken);

        postWithAuth("/api/channels/join-by-token", input)
                .andExpect(isProblem(Problem.InvalidRequestContent));
    }

    @Test
    void EditMemberAccess_InvalidInput_ReturnsBadRequest() throws Exception {
        String invalidJson = "{}";

        mockMvc.perform(put("/api/channels/10/members/2")
                        .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(isProblem(Problem.InvalidRequestContent));
    }

    @Test
    void Listen_ServiceThrowsException_CompletesWithError() throws Exception {
        when(channelService.getAccessType(1L, 1L, 10L)).thenReturn(Either.success(AccessType.READ_WRITE));

        Mockito.doThrow(new RuntimeException("Redis connection failed"))
                .when(messageEventService).addEmitter(anyLong(), anyLong(), any());

        getWithAuth("/api/channels/10/listen")
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk());
    }
}