package pt.isel.domain.common;

public sealed interface MessageError extends AppError {
    record EmptyMessage() implements MessageError {
    }

    record InvalidMessageLength() implements MessageError {
    }

    record ChannelNotFound() implements MessageError {
    }

    record InvalidLimit() implements MessageError {
    }

    record InvalidOffset() implements MessageError {
    }

    record MessagesNotFound() implements MessageError {
    }

    record UserNotAuthorized() implements MessageError {
    }

    record UserNotFound() implements MessageError {
    }

    record UserNotInChannel() implements MessageError {
    }

    record RateLimitExceeded() implements MessageError {
    }
}