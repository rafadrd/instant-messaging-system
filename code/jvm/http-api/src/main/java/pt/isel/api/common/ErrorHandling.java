package pt.isel.api.common;

import org.springframework.http.ResponseEntity;
import pt.isel.domain.common.*;

import java.util.function.Function;

public class ErrorHandling {

    public static <E extends AppError, T> ResponseEntity<?> handleResult(Either<E, T> result) {
        return handleResult(result, t -> ResponseEntity.ok(t));
    }

    public static <E extends AppError, T> ResponseEntity<?> handleResult(Either<E, T> result, Function<T, ResponseEntity<?>> onSuccess) {
        if (result instanceof Either.Right<E, T>(T value1)) {
            return onSuccess.apply(value1);
        } else if (result instanceof Either.Left<E, T>(E value)) {
            return toProblem(value).toResponseEntity();
        }
        throw new IllegalStateException("Unknown result type");
    }

    private static Problem toProblem(AppError error) {
        return switch (error) {
            case UserError e -> mapUserError(e);
            case ChannelError e -> mapChannelError(e);
            case MessageError e -> mapMessageError(e);
            case InvitationError e -> mapInvitationError(e);
        };
    }

    private static Problem mapUserError(UserError error) {
        return switch (error) {
            case UserError.EmptyToken e -> Problem.EmptyToken;
            case UserError.UserNotFound e -> Problem.UserNotFound;
            case UserError.EmptyUsername e -> Problem.EmptyUsername;
            case UserError.InvalidUsernameLength e -> Problem.InvalidUsernameLength;
            case UserError.EmptyPassword e -> Problem.EmptyPassword;
            case UserError.InsecurePassword e -> Problem.InsecurePassword;
            case UserError.IncorrectPassword e -> Problem.IncorrectPassword;
            case UserError.InvitationExpired e -> Problem.InvitationExpired;
            case UserError.InvitationNotFound e -> Problem.InvitationNotFound;
            case UserError.UsernameAlreadyInUse e -> Problem.UsernameAlreadyInUse;
            case UserError.UserHasOwnedChannels e -> Problem.UserHasOwnedChannels;
            case UserError.InvitationAlreadyUsed e -> Problem.InvitationAlreadyUsed;
            case UserError.PasswordSameAsPrevious e -> Problem.PasswordSameAsPrevious;
        };
    }

    private static Problem mapChannelError(ChannelError error) {
        return switch (error) {
            case ChannelError.EmptyToken e -> Problem.EmptyToken;
            case ChannelError.UserIsOwner e -> Problem.UserIsOwner;
            case ChannelError.UserNotOwner e -> Problem.UserNotOwner;
            case ChannelError.UserNotFound e -> Problem.UserNotFound;
            case ChannelError.TokenNotFound e -> Problem.TokenNotFound;
            case ChannelError.InvalidAction e -> Problem.InvalidAction;
            case ChannelError.EmptyAccessType e -> Problem.EmptyAccessType;
            case ChannelError.ChannelNotFound e -> Problem.ChannelNotFound;
            case ChannelError.UserNotInChannel e -> Problem.UserNotInChannel;
            case ChannelError.NoJoinedChannels e -> Problem.NoJoinedChannels;
            case ChannelError.OwnerCannotLeave e -> Problem.OwnerCannotLeave;
            case ChannelError.EmptyChannelName e -> Problem.EmptyChannelName;
            case ChannelError.ChannelIsPrivate e -> Problem.ChannelIsPrivate;
            case ChannelError.InvalidChannelNameLength e -> Problem.InvalidChannelNameLength;
            case ChannelError.InvalidLimit e -> Problem.InvalidLimit;
            case ChannelError.InvalidOffset e -> Problem.InvalidOffset;
            case ChannelError.UserNotAuthorized e -> Problem.UserNotAuthorized;
            case ChannelError.InvitationExpired e -> Problem.InvitationExpired;
            case ChannelError.NoMatchingChannels e -> Problem.NoMatchingChannels;
            case ChannelError.ChannelAlreadyExists e -> Problem.ChannelAlreadyExists;
            case ChannelError.UserAlreadyInChannel e -> Problem.UserAlreadyInChannel;
            case ChannelError.InvitationAlreadyUsed e -> Problem.InvitationAlreadyUsed;
        };
    }

    private static Problem mapMessageError(MessageError error) {
        return switch (error) {
            case MessageError.UserNotFound e -> Problem.UserNotFound;
            case MessageError.InvalidLimit e -> Problem.InvalidLimit;
            case MessageError.EmptyMessage e -> Problem.EmptyMessage;
            case MessageError.InvalidMessageLength e -> Problem.InvalidMessageLength;
            case MessageError.InvalidOffset e -> Problem.InvalidOffset;
            case MessageError.ChannelNotFound e -> Problem.ChannelNotFound;
            case MessageError.UserNotInChannel e -> Problem.UserNotInChannel;
            case MessageError.MessagesNotFound e -> Problem.MessagesNotFound;
            case MessageError.UserNotAuthorized e -> Problem.UserNotAuthorized;
        };
    }

    private static Problem mapInvitationError(InvitationError error) {
        return switch (error) {
            case InvitationError.UserNotFound e -> Problem.UserNotFound;
            case InvitationError.ChannelNotFound e -> Problem.ChannelNotFound;
            case InvitationError.UserNotInChannel e -> Problem.UserNotInChannel;
            case InvitationError.UserNotAuthorized e -> Problem.UserNotAuthorized;
            case InvitationError.InvitationNotFound e -> Problem.InvitationNotFound;
            case InvitationError.InvalidExpirationTime e -> Problem.InvalidExpirationTime;
            case InvitationError.InvitationAlreadyExists e -> Problem.InvitationAlreadyExists;
        };
    }
}