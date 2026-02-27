package pt.isel.api

import org.springframework.http.ResponseEntity
import pt.isel.services.AppError
import pt.isel.services.ChannelError
import pt.isel.services.Either
import pt.isel.services.Failure
import pt.isel.services.InvitationError
import pt.isel.services.MessageError
import pt.isel.services.Success
import pt.isel.services.UserError

internal fun <T> handleResult(
    result: Either<AppError, T>,
    onSuccess: (T) -> ResponseEntity<*> = { ResponseEntity.ok(it) },
): ResponseEntity<*> =
    when (result) {
        is Success -> onSuccess(result.value)
        is Failure -> result.value.toProblem().toResponseEntity()
    }

internal fun AppError.toProblem(): Problem =
    when (this) {
        is UserError -> toProblem()
        is ChannelError -> toProblem()
        is MessageError -> toProblem()
        is InvitationError -> toProblem()
    }

private fun UserError.toProblem(): Problem =
    when (this) {
        UserError.EmptyToken -> Problem.EmptyToken
        UserError.UserNotFound -> Problem.UserNotFound
        UserError.EmptyUsername -> Problem.EmptyUsername
        UserError.InvalidUsernameLength -> Problem.InvalidUsernameLength
        UserError.EmptyPassword -> Problem.EmptyPassword
        UserError.InsecurePassword -> Problem.InsecurePassword
        UserError.IncorrectPassword -> Problem.IncorrectPassword
        UserError.InvitationExpired -> Problem.InvitationExpired
        UserError.InvitationNotFound -> Problem.InvitationNotFound
        UserError.UsernameAlreadyInUse -> Problem.UsernameAlreadyInUse
        UserError.UserHasOwnedChannels -> Problem.UserHasOwnedChannels
        UserError.InvitationAlreadyUsed -> Problem.InvitationAlreadyUsed
        UserError.PasswordSameAsPrevious -> Problem.PasswordSameAsPrevious
    }

private fun ChannelError.toProblem(): Problem =
    when (this) {
        ChannelError.EmptyToken -> Problem.EmptyToken
        ChannelError.UserIsOwner -> Problem.UserIsOwner
        ChannelError.UserNotOwner -> Problem.UserNotOwner
        ChannelError.UserNotFound -> Problem.UserNotFound
        ChannelError.TokenNotFound -> Problem.TokenNotFound
        ChannelError.InvalidAction -> Problem.InvalidAction
        ChannelError.EmptyAccessType -> Problem.EmptyAccessType
        ChannelError.ChannelNotFound -> Problem.ChannelNotFound
        ChannelError.UserNotInChannel -> Problem.UserNotInChannel
        ChannelError.NoJoinedChannels -> Problem.NoJoinedChannels
        ChannelError.OwnerCannotLeave -> Problem.OwnerCannotLeave
        ChannelError.EmptyChannelName -> Problem.EmptyChannelName
        ChannelError.InvalidChannelNameLength -> Problem.InvalidChannelNameLength
        ChannelError.InvalidLimit -> Problem.InvalidLimit
        ChannelError.InvalidOffset -> Problem.InvalidOffset
        ChannelError.UserNotAuthorized -> Problem.UserNotAuthorized
        ChannelError.InvitationExpired -> Problem.InvitationExpired
        ChannelError.NoMatchingChannels -> Problem.NoMatchingChannels
        ChannelError.ChannelAlreadyExists -> Problem.ChannelAlreadyExists
        ChannelError.UserAlreadyInChannel -> Problem.UserAlreadyInChannel
        ChannelError.InvitationAlreadyUsed -> Problem.InvitationAlreadyUsed
    }

private fun MessageError.toProblem(): Problem =
    when (this) {
        MessageError.UserNotFound -> Problem.UserNotFound
        MessageError.InvalidLimit -> Problem.InvalidLimit
        MessageError.EmptyMessage -> Problem.EmptyMessage
        MessageError.InvalidMessageLength -> Problem.InvalidMessageLength
        MessageError.InvalidOffset -> Problem.InvalidOffset
        MessageError.ChannelNotFound -> Problem.ChannelNotFound
        MessageError.UserNotInChannel -> Problem.UserNotInChannel
        MessageError.MessagesNotFound -> Problem.MessagesNotFound
        MessageError.UserNotAuthorized -> Problem.UserNotAuthorized
    }

private fun InvitationError.toProblem(): Problem =
    when (this) {
        InvitationError.UserNotFound -> Problem.UserNotFound
        InvitationError.ChannelNotFound -> Problem.ChannelNotFound
        InvitationError.UserNotInChannel -> Problem.UserNotInChannel
        InvitationError.UserNotAuthorized -> Problem.UserNotAuthorized
        InvitationError.InvitationNotFound -> Problem.InvitationNotFound
        InvitationError.InvalidExpirationTime -> Problem.InvalidExpirationTime
        InvitationError.InvitationAlreadyExists -> Problem.InvitationAlreadyExists
    }
