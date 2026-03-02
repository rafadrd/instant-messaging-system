package pt.isel.domain.common;

public sealed interface InvitationError extends AppError {
    record ChannelNotFound() implements InvitationError {
    }

    record InvalidExpirationTime() implements InvitationError {
    }

    record InvitationAlreadyExists() implements InvitationError {
    }

    record InvitationNotFound() implements InvitationError {
    }

    record UserNotAuthorized() implements InvitationError {
    }

    record UserNotFound() implements InvitationError {
    }

    record UserNotInChannel() implements InvitationError {
    }
}