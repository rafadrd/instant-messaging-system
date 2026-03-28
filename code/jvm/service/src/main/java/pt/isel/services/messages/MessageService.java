package pt.isel.services.messages;

import jakarta.inject.Named;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.common.Either;
import pt.isel.domain.common.MessageError;
import pt.isel.domain.messages.Message;
import pt.isel.domain.messages.UpdatedMessage;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.Transaction;
import pt.isel.repositories.TransactionManager;
import pt.isel.services.common.RateLimiter;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Named
public class MessageService {
    private final TransactionManager trxManager;
    private final MessageEventService messageEventService;
    private final RateLimiter rateLimiter;
    private final Clock clock;

    public MessageService(TransactionManager trxManager, MessageEventService messageEventService, RateLimiter rateLimiter, Clock clock) {
        this.trxManager = trxManager;
        this.messageEventService = messageEventService;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    public Either<MessageError, Message> createMessage(String content, Long userId, Long channelId) {
        if (rateLimiter.isRateLimited("message", userId.toString(), 30, Duration.ofMinutes(1))) {
            return Either.failure(new MessageError.RateLimitExceeded());
        }

        if (content == null || content.isBlank()) return Either.failure(new MessageError.EmptyMessage());
        if (content.length() > 1000) return Either.failure(new MessageError.InvalidMessageLength());

        var result = trxManager.run(trx -> checkUserCanPostMessage(trx, userId, channelId)
                .flatMap(pair -> {
                    var message = trx.repoMessages().create(content, pair.userInfo(), pair.channel(), LocalDateTime.now(clock));
                    return Either.success(message);
                }));

        if (result instanceof Either.Right<MessageError, Message>(var message)) {
            messageEventService.broadcastMessage(channelId, new UpdatedMessage.NewMessage(message));
        }

        return result;
    }

    public Either<MessageError, List<Message>> getMessagesInChannel(Long userId, Long channelId, int limit, int offset) {
        if (limit <= 0) return Either.failure(new MessageError.InvalidLimit());
        if (offset < 0) return Either.failure(new MessageError.InvalidOffset());

        return trxManager.run(trx -> {
            Channel channel = trx.repoChannels().findById(channelId);
            if (channel == null) return Either.failure(new MessageError.ChannelNotFound());

            if (trx.repoMemberships().findUserInChannel(userId, channelId) == null) {
                return Either.failure(new MessageError.UserNotInChannel());
            }

            List<Message> messages = trx.repoMessages().findAllInChannel(channel, limit, offset);
            return Either.success(messages);
        });
    }

    private Either<MessageError, UserChannelPair> checkUserCanPostMessage(Transaction trx, Long userId, Long channelId) {
        var user = trx.repoUsers().findById(userId);
        if (user == null) return Either.failure(new MessageError.UserNotFound());

        var channel = trx.repoChannels().findById(channelId);
        if (channel == null) return Either.failure(new MessageError.ChannelNotFound());

        var membership = trx.repoMemberships().findUserInChannel(user.id(), channel.id());
        if (membership == null) return Either.failure(new MessageError.UserNotInChannel());

        if (membership.accessType() != AccessType.READ_WRITE) {
            return Either.failure(new MessageError.UserNotAuthorized());
        }

        UserInfo userInfo = new UserInfo(user.id(), user.username());
        return Either.success(new UserChannelPair(userInfo, channel));
    }

    private record UserChannelPair(UserInfo userInfo, Channel channel) {
    }
}