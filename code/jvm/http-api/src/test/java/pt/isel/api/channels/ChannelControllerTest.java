package pt.isel.api.channels;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.isel.api.TestConfig;
import pt.isel.domain.builders.ChannelBuilder;
import pt.isel.domain.builders.ChannelMemberBuilder;
import pt.isel.domain.builders.UserInfoBuilder;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.channels.ChannelMember;
import pt.isel.domain.common.Either;
import pt.isel.services.channels.ChannelService;
import pt.isel.services.messages.MessageEventService;
import pt.isel.services.users.TicketService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChannelController.class)
@Import(TestConfig.class)
class ChannelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChannelService channelService;

    @MockitoBean
    private MessageEventService messageEventService;

    @MockitoBean
    private TicketService ticketService;

    @Test
    void testGetChannels() throws Exception {
        when(channelService.searchChannels(anyString(), anyInt(), anyInt())).thenReturn(Either.success(List.of()));

        mockMvc.perform(get("/api/channels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testCreateChannel() throws Exception {
        ChannelInput input = new ChannelInput("General", true);
        Channel channel = new ChannelBuilder().withId(10L).withName("General").build();

        when(channelService.createChannel(anyString(), anyLong(), anyBoolean())).thenReturn(Either.success(channel));

        mockMvc.perform(post("/api/channels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("General"));
    }

    @Test
    void testGetChannelById() throws Exception {
        Channel channel = new ChannelBuilder().withId(10L).build();
        when(channelService.getChannelById(10L)).thenReturn(Either.success(channel));

        mockMvc.perform(get("/api/channels/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void testEditChannel() throws Exception {
        EditChannelInput input = new EditChannelInput("NewName", false);
        Channel channel = new ChannelBuilder().withId(10L).withName("NewName").withIsPublic(false).build();

        when(channelService.editChannel(eq(1L), eq(10L), eq("NewName"), eq(false))).thenReturn(Either.success(channel));

        mockMvc.perform(put("/api/channels/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("NewName"));
    }

    @Test
    void testDeleteChannel() throws Exception {
        when(channelService.deleteChannel(1L, 10L)).thenReturn(Either.success("Deleted"));

        mockMvc.perform(delete("/api/channels/10"))
                .andExpect(status().isOk());
    }

    @Test
    void testJoinChannel() throws Exception {
        when(channelService.joinPublicChannel(1L, 10L)).thenReturn(Either.success("Joined"));

        mockMvc.perform(post("/api/channels/10/join"))
                .andExpect(status().isOk());
    }

    @Test
    void testJoinChannelByToken() throws Exception {
        JoinByTokenInput input = new JoinByTokenInput("token123");
        when(channelService.joinPrivateChannel(1L, "token123")).thenReturn(Either.success("Joined"));

        mockMvc.perform(post("/api/channels/join-by-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk());
    }

    @Test
    void testLeaveChannel() throws Exception {
        when(channelService.leaveChannel(10L, 1L)).thenReturn(Either.success("Left"));

        mockMvc.perform(post("/api/channels/10/leave"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetMembers() throws Exception {
        when(channelService.getUsersInChannel(eq(10L), anyInt(), anyInt())).thenReturn(Either.success(List.of()));

        mockMvc.perform(get("/api/channels/10/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetAccessType() throws Exception {
        when(channelService.getAccessType(1L, 2L, 10L)).thenReturn(Either.success(AccessType.READ_ONLY));

        mockMvc.perform(get("/api/channels/10/members/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("READ_ONLY"));
    }

    @Test
    void testEditMemberAccess() throws Exception {
        EditMemberInput input = new EditMemberInput(AccessType.READ_WRITE);
        ChannelMember member = new ChannelMemberBuilder()
                .withId(1L)
                .withUser(new UserInfoBuilder().withId(2L).withUsername("bob").build())
                .withAccessType(AccessType.READ_WRITE)
                .build();

        when(channelService.editMemberAccess(1L, 10L, 2L, AccessType.READ_WRITE)).thenReturn(Either.success(member));

        mockMvc.perform(put("/api/channels/10/members/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessType").value("READ_WRITE"));
    }

    @Test
    void testGetSocketTicket() throws Exception {
        when(channelService.getAccessType(1L, 1L, 10L)).thenReturn(Either.success(AccessType.READ_WRITE));
        when(ticketService.createTicket(1L)).thenReturn("ticket-uuid");

        mockMvc.perform(post("/api/channels/10/socket-ticket"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket").value("ticket-uuid"));
    }

    @Test
    void testListenSse() throws Exception {
        when(channelService.getAccessType(1L, 1L, 10L)).thenReturn(Either.success(AccessType.READ_WRITE));

        mockMvc.perform(get("/api/channels/10/listen"))
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk());
    }

    @Test
    void testCreateChannelValidationFailure() throws Exception {
        ChannelInput input = new ChannelInput("ThisChannelNameIsWayTooLongToBeValid", true);

        mockMvc.perform(post("/api/channels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Request Content"));
    }

    @Test
    void testJoinChannelByTokenValidationFailure() throws Exception {
        JoinByTokenInput input = new JoinByTokenInput("");

        mockMvc.perform(post("/api/channels/join-by-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testEditMemberAccessValidationFailure() throws Exception {
        String invalidJson = "{}";

        mockMvc.perform(put("/api/channels/10/members/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testListenSseCatchesException() throws Exception {
        when(channelService.getAccessType(1L, 1L, 10L)).thenReturn(Either.success(AccessType.READ_WRITE));

        Mockito.doThrow(new RuntimeException("Redis connection failed"))
                .when(messageEventService).addEmitter(anyLong(), anyLong(), any());

        mockMvc.perform(get("/api/channels/10/listen"))
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk());
    }
}