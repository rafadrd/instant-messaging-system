package pt.isel.services

sealed interface AppError

sealed class UserError : AppError {
    data object UserHasOwnedChannels : UserError()

    data object UserNotFound : UserError()

    data object UsernameAlreadyInUse : UserError()

    data object EmptyUsername : UserError()

    data object EmptyPassword : UserError()

    data object EmptyToken : UserError()

    data object IncorrectPassword : UserError()

    data object InsecurePassword : UserError()

    data object InvitationNotFound : UserError()

    data object InvitationExpired : UserError()

    data object InvitationAlreadyUsed : UserError()

    data object PasswordSameAsPrevious : UserError()
}

sealed class MessageError : AppError {
    data object EmptyMessage : MessageError()

    data object ChannelNotFound : MessageError()

    data object InvalidLimit : MessageError()

    data object InvalidOffset : MessageError()

    data object MessagesNotFound : MessageError()

    data object UserNotAuthorized : MessageError()

    data object UserNotFound : MessageError()

    data object UserNotInChannel : MessageError()
}

sealed class InvitationError : AppError {
    data object ChannelNotFound : InvitationError()

    data object InvalidExpirationTime : InvitationError()

    data object InvitationAlreadyExists : InvitationError()

    data object InvitationNotFound : InvitationError()

    data object UserNotAuthorized : InvitationError()

    data object UserNotFound : InvitationError()

    data object UserNotInChannel : InvitationError()
}

sealed class ChannelError : AppError {
    data object UserNotAuthorized : ChannelError()

    data object UserNotOwner : ChannelError()

    data object UserIsOwner : ChannelError()

    data object InvitationAlreadyUsed : ChannelError()

    data object ChannelAlreadyExists : ChannelError()

    data object ChannelNotFound : ChannelError()

    data object EmptyToken : ChannelError()

    data object EmptyChannelName : ChannelError()

    data object EmptyAccessType : ChannelError()

    data object InvalidAction : ChannelError()

    data object InvitationExpired : ChannelError()

    data object NoJoinedChannels : ChannelError()

    data object NoMatchingChannels : ChannelError()

    data object OwnerCannotLeave : ChannelError()

    data object TokenNotFound : ChannelError()

    data object UserAlreadyInChannel : ChannelError()

    data object UserNotFound : ChannelError()

    data object UserNotInChannel : ChannelError()
}
