package pt.isel.api.common

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.net.URI

private const val MEDIA_TYPE = "application/problem+json"
private const val PROBLEM_URI_PATH =
    "https://github.com/isel-leic-daw/2024-daw-leic53d-g02-53d/tree/main/docs/instantMessaging"

data class ProblemResponse(
    val type: String,
    val title: String,
    val status: Int,
    val detail: String? = null,
)

sealed class Problem(
    val type: URI,
    val title: String,
    val status: HttpStatus,
    val detail: String? = null,
) {
    fun toResponseEntity(): ResponseEntity<ProblemResponse> =
        ResponseEntity
            .status(status)
            .header("Content-Type", MEDIA_TYPE)
            .body(ProblemResponse(type.toString(), title, status.value(), detail))

    companion object {
        private fun createUri(path: String) = URI("$PROBLEM_URI_PATH/$path")
    }

    data object ChannelAlreadyExists : Problem(
        type = createUri("channel-already-exists"),
        title = "Channel Already Exists",
        status = HttpStatus.CONFLICT,
        detail = "The channel already exists.",
    )

    data object ChannelNotFound : Problem(
        type = createUri("channel-not-found"),
        title = "Channel Not Found",
        status = HttpStatus.NOT_FOUND,
        detail = "The channel was not found.",
    )

    data object EmptyAccessType : Problem(
        type = createUri("empty-access-type"),
        title = "Empty Access Type",
        status = HttpStatus.BAD_REQUEST,
        detail = "The access type is empty.",
    )

    data object EmptyChannelName : Problem(
        type = createUri("empty-channel-name"),
        title = "Empty Channel Name",
        status = HttpStatus.BAD_REQUEST,
        detail = "The channel name is empty.",
    )

    data object InvalidChannelNameLength : Problem(
        type = createUri("invalid-channel-name-length"),
        title = "Invalid Channel Name Length",
        status = HttpStatus.BAD_REQUEST,
        detail = "The channel name must be between 1 and 30 characters.",
    )

    data object EmptyMessage : Problem(
        type = createUri("empty-message"),
        title = "Empty Message",
        status = HttpStatus.BAD_REQUEST,
        detail = "The message is empty.",
    )

    data object InvalidMessageLength : Problem(
        type = createUri("invalid-message-length"),
        title = "Invalid Message Length",
        status = HttpStatus.BAD_REQUEST,
        detail = "The message content must be between 1 and 1000 characters.",
    )

    data object EmptyPassword : Problem(
        type = createUri("empty-password"),
        title = "Empty Password",
        status = HttpStatus.BAD_REQUEST,
        detail = "The password is empty.",
    )

    data object EmptyToken : Problem(
        type = createUri("empty-token"),
        title = "Empty Token",
        status = HttpStatus.BAD_REQUEST,
        detail = "The token is empty.",
    )

    data object EmptyUsername : Problem(
        type = createUri("empty-username"),
        title = "Empty Username",
        status = HttpStatus.BAD_REQUEST,
        detail = "The username is empty.",
    )

    data object InvalidUsernameLength : Problem(
        type = createUri("invalid-username-length"),
        title = "Invalid Username Length",
        status = HttpStatus.BAD_REQUEST,
        detail = "The username must be between 1 and 30 characters.",
    )

    data object IncorrectPassword : Problem(
        type = createUri("incorrect-password"),
        title = "Incorrect Password",
        status = HttpStatus.UNAUTHORIZED,
        detail = "The password is incorrect.",
    )

    data object InsecurePassword : Problem(
        type = createUri("insecure-password"),
        title = "Insecure Password",
        status = HttpStatus.BAD_REQUEST,
        detail = "The password is insecure.",
    )

    data object InvalidAction : Problem(
        type = createUri("invalid-action"),
        title = "Invalid Action",
        status = HttpStatus.BAD_REQUEST,
        detail = "The action is invalid.",
    )

    data object InvalidExpirationTime : Problem(
        type = createUri("invalid-expiration-time"),
        title = "Invalid Expiration Time",
        status = HttpStatus.BAD_REQUEST,
        detail = "The expiration time is invalid.",
    )

    data object InvalidLimit : Problem(
        type = createUri("invalid-limit"),
        title = "Invalid Limit",
        status = HttpStatus.BAD_REQUEST,
        detail = "The limit is invalid.",
    )

    data object InvalidOffset : Problem(
        type = createUri("invalid-offset"),
        title = "Invalid Offset",
        status = HttpStatus.BAD_REQUEST,
        detail = "The offset is invalid.",
    )

    data object InvalidRequestContent : Problem(
        type = createUri("invalid-request-content"),
        title = "Invalid Request Content",
        status = HttpStatus.BAD_REQUEST,
        detail = "The request content is invalid.",
    )

    data object InvitationAlreadyExists : Problem(
        type = createUri("invitation-already-exists"),
        title = "Invitation Already Exists",
        status = HttpStatus.CONFLICT,
        detail = "The invitation already exists.",
    )

    data object InvitationAlreadyUsed : Problem(
        type = createUri("invitation-already-used"),
        title = "Invitation Already Used",
        status = HttpStatus.BAD_REQUEST,
        detail = "The invitation has already been used.",
    )

    data object InvitationExpired : Problem(
        type = createUri("invitation-expired"),
        title = "Invitation Expired",
        status = HttpStatus.BAD_REQUEST,
        detail = "The invitation has expired.",
    )

    data object InvitationNotFound : Problem(
        type = createUri("invitation-not-found"),
        title = "Invitation Not Found",
        status = HttpStatus.NOT_FOUND,
        detail = "The invitation was not found.",
    )

    data object MessagesNotFound : Problem(
        type = createUri("messages-not-found"),
        title = "Messages Not Found",
        status = HttpStatus.NOT_FOUND,
        detail = "The messages were not found.",
    )

    data object NoJoinedChannels : Problem(
        type = createUri("no-joined-channels"),
        title = "No Joined Channels",
        status = HttpStatus.NOT_FOUND,
        detail = "The user has not joined any channel.",
    )

    data object NoMatchingChannels : Problem(
        type = createUri("no-matching-channels"),
        title = "No Matching Channels",
        status = HttpStatus.NOT_FOUND,
        detail = "The channel name does not match any channel.",
    )

    data object OwnerCannotLeave : Problem(
        type = createUri("owner-cannot-leave"),
        title = "Owner Cannot Leave",
        status = HttpStatus.FORBIDDEN,
        detail = "The owner cannot leave the channel.",
    )

    data object PasswordSameAsPrevious : Problem(
        type = createUri("password-same-as-previous"),
        title = "Password Same As Previous",
        status = HttpStatus.BAD_REQUEST,
        detail = "The password is the same as the previous one.",
    )

    data object TokenNotFound : Problem(
        type = createUri("token-not-found"),
        title = "Token Not Found",
        status = HttpStatus.NOT_FOUND,
        detail = "The token was not found.",
    )

    data object UserAlreadyInChannel : Problem(
        type = createUri("user-already-in-channel"),
        title = "User Already In Channel",
        status = HttpStatus.FORBIDDEN,
        detail = "The user is already in the channel.",
    )

    data object UserIsOwner : Problem(
        type = createUri("user-is-owner"),
        title = "User Is Owner",
        status = HttpStatus.BAD_REQUEST,
        detail = "The user is the owner of the channel.",
    )

    data object UserHasOwnedChannels : Problem(
        type = createUri("user-has-owned-channels"),
        title = "User Has Owned Channels",
        status = HttpStatus.BAD_REQUEST,
        detail = "The user has owned channels.",
    )

    data object UserNotAuthorized : Problem(
        type = createUri("user-not-authorized"),
        title = "User Not Authorized",
        status = HttpStatus.UNAUTHORIZED,
        detail = "The user is not authorized.",
    )

    data object UserNotFound : Problem(
        type = createUri("user-not-found"),
        title = "User Not Found",
        status = HttpStatus.NOT_FOUND,
        detail = "The user was not found.",
    )

    data object UserNotInChannel : Problem(
        type = createUri("user-not-in-channel"),
        title = "User Not In Channel",
        status = HttpStatus.FORBIDDEN,
        detail = "The user is not in the channel.",
    )

    data object UserNotOwner : Problem(
        type = createUri("user-not-owner"),
        title = "User Not Owner",
        status = HttpStatus.BAD_REQUEST,
        detail = "The user is not the owner of the channel.",
    )

    data object UsernameAlreadyInUse : Problem(
        type = createUri("username-already-in-use"),
        title = "Username Already In Use",
        status = HttpStatus.CONFLICT,
        detail = "The username is already in use.",
    )

    data object InternalServerError : Problem(
        type = createUri("internal-server-error"),
        title = "Internal Server Error",
        status = HttpStatus.INTERNAL_SERVER_ERROR,
        detail = "An internal server error occurred.",
    )
}
