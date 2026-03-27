package pt.isel.services.messages;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.common.Either;
import pt.isel.domain.common.MessageError;
import pt.isel.domain.messages.Message;
import pt.isel.domain.messages.UpdatedMessage;
import pt.isel.domain.messages.UpdatedMessageEmitter;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.User;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.mem.TransactionManagerInMem;
import pt.isel.services.common.RateLimiter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MessageServiceTest {

    private TransactionManagerInMem trxManager;
    private MessageService messageService;

    private User alice;
    private User bob;
    private User charlie;
    private Channel channel;

    private boolean rateLimitTriggered = false;
    private boolean broadcastTriggered = false;

    @BeforeEach
    void setUp() {
        trxManager = new TransactionManagerInMem();
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

        messageService = new MessageService(trxManager, eventService, rateLimiter);

        alice = trxManager.run(trx -> trx.repoUsers().create("alice", new PasswordValidationInfo("hash")));
        bob = trxManager.run(trx -> trx.repoUsers().create("bob", new PasswordValidationInfo("hash")));
        charlie = trxManager.run(trx -> trx.repoUsers().create("charlie", new PasswordValidationInfo("hash")));

        channel = trxManager.run(trx -> {
            UserInfo aliceInfo = new UserInfo(alice.id(), alice.username());
            Channel c = trx.repoChannels().create("General", aliceInfo, true);
            trx.repoMemberships().addUserToChannel(aliceInfo, c, AccessType.READ_WRITE);
            trx.repoMemberships().addUserToChannel(new UserInfo(bob.id(), bob.username()), c, AccessType.READ_WRITE);
            trx.repoMemberships().addUserToChannel(new UserInfo(charlie.id(), charlie.username()), c, AccessType.READ_ONLY);
            return c;
        });
    }

    @Test
    void testCreateMessage_Success() {
        Either<MessageError, Message> result = messageService.createMessage("Hello World", alice.id(), channel.id());

        assertThat(result).isInstanceOf(Either.Right.class);
        Message msg = ((Either.Right<MessageError, Message>) result).value();
        assertThat(msg.content()).isEqualTo("Hello World");
        assertThat(msg.user().id()).isEqualTo(alice.id());
        assertThat(broadcastTriggered).isTrue();
    }

    @Test
    void testCreateMessage_RateLimited() {
        rateLimitTriggered = true;
        Either<MessageError, Message> result = messageService.createMessage("Hello", alice.id(), channel.id());

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<MessageError, Message>) result).value()).isInstanceOf(MessageError.RateLimitExceeded.class);
        assertThat(broadcastTriggered).isFalse();
    }

    @Test
    void testCreateMessage_EmptyContent() {
        assertThat(messageService.createMessage("", alice.id(), channel.id())).isInstanceOf(Either.Left.class);
        assertThat(messageService.createMessage(null, alice.id(), channel.id())).isInstanceOf(Either.Left.class);
    }

    @Test
    void testCreateMessage_ContentTooLong() {
        String longMsg = "a".repeat(1001);
        Either<MessageError, Message> result = messageService.createMessage(longMsg, alice.id(), channel.id());

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<MessageError, Message>) result).value()).isInstanceOf(MessageError.InvalidMessageLength.class);
    }

    @Test
    void testCreateMessage_UserNotAuthorized() {
        Either<MessageError, Message> result = messageService.createMessage("Hello", charlie.id(), channel.id());

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<MessageError, Message>) result).value()).isInstanceOf(MessageError.UserNotAuthorized.class);
    }

    @Test
    void testGetMessagesInChannel_Success() {
        messageService.createMessage("Msg 1", alice.id(), channel.id());
        messageService.createMessage("Msg 2", bob.id(), channel.id());

        Either<MessageError, List<Message>> result = messageService.getMessagesInChannel(charlie.id(), channel.id(), 10, 0);

        assertThat(result).isInstanceOf(Either.Right.class);
        assertThat(((Either.Right<MessageError, List<Message>>) result).value()).hasSize(2);
    }

    @Test
    void testGetMessagesInChannel_InvalidPagination() {
        assertThat(messageService.getMessagesInChannel(alice.id(), channel.id(), 0, 0)).isInstanceOf(Either.Left.class);
        assertThat(messageService.getMessagesInChannel(alice.id(), channel.id(), 10, -1)).isInstanceOf(Either.Left.class);
    }

    @Test
    void testGetMessagesInChannel_UserNotInChannel() {
        User dave = trxManager.run(trx -> trx.repoUsers().create("dave", new PasswordValidationInfo("hash")));

        Either<MessageError, List<Message>> result = messageService.getMessagesInChannel(dave.id(), channel.id(), 10, 0);

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<MessageError, List<Message>>) result).value()).isInstanceOf(MessageError.UserNotInChannel.class);
    }

    @Test
    void testCreateMessage_UserNotFound() {
        Either<MessageError, Message> result = messageService.createMessage("Hello", 999L, channel.id());

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<MessageError, Message>) result).value()).isInstanceOf(MessageError.UserNotFound.class);
    }

    @Test
    void testCreateMessage_ChannelNotFound() {
        Either<MessageError, Message> result = messageService.createMessage("Hello", alice.id(), 999L);

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<MessageError, Message>) result).value()).isInstanceOf(MessageError.ChannelNotFound.class);
    }

    @Test
    void testCreateMessage_UserNotInChannel() {
        User dave = trxManager.run(trx -> trx.repoUsers().create("dave", new PasswordValidationInfo("hash")));
        Either<MessageError, Message> result = messageService.createMessage("Hello", dave.id(), channel.id());

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<MessageError, Message>) result).value()).isInstanceOf(MessageError.UserNotInChannel.class);
    }

    @Test
    void testGetMessagesInChannel_ChannelNotFound() {
        Either<MessageError, List<Message>> result = messageService.getMessagesInChannel(alice.id(), 999L, 10, 0);

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<MessageError, List<Message>>) result).value()).isInstanceOf(MessageError.ChannelNotFound.class);
    }
}