package pt.isel.services.messages;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.common.Either;
import pt.isel.domain.common.EitherAssert;
import pt.isel.domain.common.MessageError;
import pt.isel.domain.messages.Message;
import pt.isel.domain.messages.UpdatedMessage;
import pt.isel.domain.messages.UpdatedMessageEmitter;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.User;
import pt.isel.services.AbstractServiceTest;
import pt.isel.services.common.RateLimiter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MessageServiceTest extends AbstractServiceTest {

    private MessageService messageService;
    private Channel channel;
    private boolean rateLimitTriggered;
    private boolean broadcastTriggered;

    @BeforeEach
    void setUp() {
        super.setUpBaseState();
        rateLimitTriggered = false;
        broadcastTriggered = false;

        RateLimiter rateLimiter = (action, identifier, limit, window) -> rateLimitTriggered;

        MessageEventService eventService = new MessageEventService() {
            @Override
            public void addEmitter(Long channelId, Long userId, UpdatedMessageEmitter emitter) {
            }

            @Override
            public void broadcastMessage(Long channelId, UpdatedMessage signal) {
                broadcastTriggered = true;
            }
        };

        messageService = new MessageService(trxManager, eventService, rateLimiter, clock);
        channel = createChannelWithMembers("General", true);
    }

    @Test
    void testCreateMessage_Success() {
        Either<MessageError, Message> result = messageService.createMessage("Hello World", alice.id(), channel.id());

        Message msg = EitherAssert.assertRight(result);
        assertThat(msg.content()).isEqualTo("Hello World");
        assertThat(msg.user().id()).isEqualTo(alice.id());
        assertThat(broadcastTriggered).isTrue();
    }

    @Test
    void testCreateMessage_RateLimited() {
        rateLimitTriggered = true;
        Either<MessageError, Message> result = messageService.createMessage("Hello", alice.id(), channel.id());

        EitherAssert.assertLeft(result, MessageError.RateLimitExceeded.class);
        assertThat(broadcastTriggered).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testCreateMessage_EmptyContent(String invalidContent) {
        EitherAssert.assertLeft(messageService.createMessage(invalidContent, alice.id(), channel.id()));
    }

    @Test
    void testCreateMessage_ContentTooLong() {
        String longMsg = "a".repeat(1001);
        Either<MessageError, Message> result = messageService.createMessage(longMsg, alice.id(), channel.id());

        EitherAssert.assertLeft(result, MessageError.InvalidMessageLength.class);
    }

    @Test
    void testCreateMessage_UserNotAuthorized() {
        Either<MessageError, Message> result = messageService.createMessage("Hello", charlie.id(), channel.id());

        EitherAssert.assertLeft(result, MessageError.UserNotAuthorized.class);
    }

    @Test
    void testGetMessagesInChannel_Success() {
        messageService.createMessage("Msg 1", alice.id(), channel.id());
        messageService.createMessage("Msg 2", bob.id(), channel.id());

        Either<MessageError, List<Message>> result = messageService.getMessagesInChannel(charlie.id(), channel.id(), 10, 0);

        assertThat(EitherAssert.assertRight(result)).hasSize(2);
    }

    @Test
    void testGetMessagesInChannel_InvalidPagination() {
        EitherAssert.assertLeft(messageService.getMessagesInChannel(alice.id(), channel.id(), 0, 0));
        EitherAssert.assertLeft(messageService.getMessagesInChannel(alice.id(), channel.id(), 10, -1));
    }

    @Test
    void testGetMessagesInChannel_UserNotInChannel() {
        User dave = trxManager.run(trx -> trx.repoUsers().create("dave", new PasswordValidationInfo("hash")));

        Either<MessageError, List<Message>> result = messageService.getMessagesInChannel(dave.id(), channel.id(), 10, 0);

        EitherAssert.assertLeft(result, MessageError.UserNotInChannel.class);
    }

    @Test
    void testCreateMessage_UserNotFound() {
        Either<MessageError, Message> result = messageService.createMessage("Hello", 999L, channel.id());

        EitherAssert.assertLeft(result, MessageError.UserNotFound.class);
    }

    @Test
    void testCreateMessage_ChannelNotFound() {
        Either<MessageError, Message> result = messageService.createMessage("Hello", alice.id(), 999L);

        EitherAssert.assertLeft(result, MessageError.ChannelNotFound.class);
    }

    @Test
    void testCreateMessage_UserNotInChannel() {
        User dave = trxManager.run(trx -> trx.repoUsers().create("dave", new PasswordValidationInfo("hash")));
        Either<MessageError, Message> result = messageService.createMessage("Hello", dave.id(), channel.id());

        EitherAssert.assertLeft(result, MessageError.UserNotInChannel.class);
    }

    @Test
    void testGetMessagesInChannel_ChannelNotFound() {
        Either<MessageError, List<Message>> result = messageService.getMessagesInChannel(alice.id(), 999L, 10, 0);

        EitherAssert.assertLeft(result, MessageError.ChannelNotFound.class);
    }
}