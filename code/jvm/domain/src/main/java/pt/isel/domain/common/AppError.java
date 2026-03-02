package pt.isel.domain.common;

public sealed interface AppError permits UserError, MessageError, InvitationError, ChannelError {
}