package pt.isel.domain.common;

public sealed interface UserError extends AppError {
    record UserHasOwnedChannels() implements UserError {
    }

    record UserNotFound() implements UserError {
    }

    record UsernameAlreadyInUse() implements UserError {
    }

    record EmptyUsername() implements UserError {
    }

    record InvalidUsernameLength() implements UserError {
    }

    record EmptyPassword() implements UserError {
    }

    record EmptyToken() implements UserError {
    }

    record IncorrectPassword() implements UserError {
    }

    record InsecurePassword() implements UserError {
    }

    record InvitationNotFound() implements UserError {
    }

    record InvitationExpired() implements UserError {
    }

    record InvitationAlreadyUsed() implements UserError {
    }

    record PasswordSameAsPrevious() implements UserError {
    }

    record RateLimitExceeded() implements UserError {
    }
}