package pt.isel.services.messages;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.common.Either;
import pt.isel.domain.common.EitherAssert;
import pt.isel.domain.common.MessageError;
import pt.isel.domain.messages.Message;
import pt.isel.domain.messages.UpdatedMessage;
import pt.isel.domain.users.User;
import pt.isel.services.AbstractServiceTest;
import pt.isel.services.common.RateLimiter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest extends AbstractServiceTest {

    @Mock
    private MessageEventService messageEventService;

    @Mock
    private RateLimiter rateLimiter;

    private MessageService messageService;
    private Channel channel;

    @BeforeEach
    void setUp() {
        super.setUpBaseState();

        lenient().when(rateLimiter.isRateLimited(anyString(), anyString(), anyInt(), any())).thenReturn(false);

        messageService = new MessageService(trxManager, messageEventService, rateLimiter, clock);
        channel = createChannelWithMembers("General", true);
    }

    @Test
    void CreateMessage_ValidInput_ReturnsSuccess() {
        Either<MessageError, Message> result = messageService.createMessage("Hello World", alice.id(), channel.id());

        Message msg = EitherAssert.assertThat(result).isRight().getRightValue();
        assertThat(msg.content()).isEqualTo("Hello World");
        assertThat(msg.user().id()).isEqualTo(alice.id());
        verify(messageEventService).broadcastMessage(eq(channel.id()), any(UpdatedMessage.NewMessage.class));
    }

    @Test
    void CreateMessage_RateLimited_ReturnsLeft() {
        when(rateLimiter.isRateLimited(anyString(), anyString(), anyInt(), any())).thenReturn(true);

        Either<MessageError, Message> result = messageService.createMessage("Hello", alice.id(), channel.id());

        EitherAssert.assertThat(result).isLeftInstanceOf(MessageError.RateLimitExceeded.class);
        verify(messageEventService, never()).broadcastMessage(anyLong(), any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void CreateMessage_EmptyContent_ReturnsLeft(String invalidContent) {
        Either<MessageError, Message> result = messageService.createMessage(invalidContent, alice.id(), channel.id());

        EitherAssert.assertThat(result).isLeftInstanceOf(MessageError.EmptyMessage.class);
    }

    @Test
    void CreateMessage_ContentTooLong_ReturnsLeft() {
        String longMsg = "a".repeat(1001);

        Either<MessageError, Message> result = messageService.createMessage(longMsg, alice.id(), channel.id());

        EitherAssert.assertThat(result).isLeftInstanceOf(MessageError.InvalidMessageLength.class);
    }

    @Test
    void CreateMessage_UserNotAuthorized_ReturnsLeft() {
        Either<MessageError, Message> result = messageService.createMessage("Hello", charlie.id(), channel.id());

        EitherAssert.assertThat(result).isLeftInstanceOf(MessageError.UserNotAuthorized.class);
    }

    @Test
    void GetMessagesInChannel_ValidInput_ReturnsSuccess() {
        messageService.createMessage("Msg 1", alice.id(), channel.id());
        messageService.createMessage("Msg 2", bob.id(), channel.id());

        Either<MessageError, List<Message>> result = messageService.getMessagesInChannel(charlie.id(), channel.id(), 10, 0);

        assertThat(EitherAssert.assertThat(result).isRight().getRightValue()).hasSize(2);
    }

    @Test
    void GetMessagesInChannel_InvalidPagination_ReturnsLeft() {
        Either<MessageError, List<Message>> result1 = messageService.getMessagesInChannel(alice.id(), channel.id(), 0, 0);
        Either<MessageError, List<Message>> result2 = messageService.getMessagesInChannel(alice.id(), channel.id(), 10, -1);

        EitherAssert.assertThat(result1).isLeftInstanceOf(MessageError.InvalidLimit.class);
        EitherAssert.assertThat(result2).isLeftInstanceOf(MessageError.InvalidOffset.class);
    }

    @Test
    void GetMessagesInChannel_UserNotInChannel_ReturnsLeft() {
        User dave = trxManager.run(trx -> insertUser(trx, "dave"));

        Either<MessageError, List<Message>> result = messageService.getMessagesInChannel(dave.id(), channel.id(), 10, 0);

        EitherAssert.assertThat(result).isLeftInstanceOf(MessageError.UserNotInChannel.class);
    }

    @Test
    void CreateMessage_UserNotFound_ReturnsLeft() {
        Either<MessageError, Message> result = messageService.createMessage("Hello", 999L, channel.id());

        EitherAssert.assertThat(result).isLeftInstanceOf(MessageError.UserNotFound.class);
    }

    @Test
    void CreateMessage_ChannelNotFound_ReturnsLeft() {
        Either<MessageError, Message> result = messageService.createMessage("Hello", alice.id(), 999L);

        EitherAssert.assertThat(result).isLeftInstanceOf(MessageError.ChannelNotFound.class);
    }

    @Test
    void CreateMessage_UserNotInChannel_ReturnsLeft() {
        User dave = trxManager.run(trx -> insertUser(trx, "dave"));

        Either<MessageError, Message> result = messageService.createMessage("Hello", dave.id(), channel.id());

        EitherAssert.assertThat(result).isLeftInstanceOf(MessageError.UserNotInChannel.class);
    }

    @Test
    void GetMessagesInChannel_ChannelNotFound_ReturnsLeft() {
        Either<MessageError, List<Message>> result = messageService.getMessagesInChannel(alice.id(), 999L, 10, 0);

        EitherAssert.assertThat(result).isLeftInstanceOf(MessageError.ChannelNotFound.class);
    }
}