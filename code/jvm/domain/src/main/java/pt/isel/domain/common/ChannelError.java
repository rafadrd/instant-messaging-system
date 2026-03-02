package pt.isel.domain.common;

public sealed interface ChannelError extends AppError {
    record UserNotAuthorized() implements ChannelError {
    }

    record UserNotOwner() implements ChannelError {
    }

    record UserIsOwner() implements ChannelError {
    }

    record ChannelIsPrivate() implements ChannelError {
    }

    record InvitationAlreadyUsed() implements ChannelError {
    }

    record ChannelAlreadyExists() implements ChannelError {
    }

    record ChannelNotFound() implements ChannelError {
    }

    record EmptyToken() implements ChannelError {
    }

    record EmptyChannelName() implements ChannelError {
    }

    record InvalidChannelNameLength() implements ChannelError {
    }

    record EmptyAccessType() implements ChannelError {
    }

    record InvalidAction() implements ChannelError {
    }

    record InvalidLimit() implements ChannelError {
    }

    record InvalidOffset() implements ChannelError {
    }

    record InvitationExpired() implements ChannelError {
    }

    record NoJoinedChannels() implements ChannelError {
    }

    record NoMatchingChannels() implements ChannelError {
    }

    record OwnerCannotLeave() implements ChannelError {
    }

    record TokenNotFound() implements ChannelError {
    }

    record UserAlreadyInChannel() implements ChannelError {
    }

    record UserNotFound() implements ChannelError {
    }

    record UserNotInChannel() implements ChannelError {
    }
}