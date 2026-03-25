package pt.isel.api.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;

public class Problem {
    public static final Problem ChannelAlreadyExists = new Problem("channel-already-exists", "Channel Already Exists", HttpStatus.CONFLICT, "The channel already exists.");
    public static final Problem ChannelNotFound = new Problem("channel-not-found", "Channel Not Found", HttpStatus.NOT_FOUND, "The channel was not found.");
    public static final Problem ChannelIsPrivate = new Problem("channel-is-private", "Channel Is Private", HttpStatus.FORBIDDEN, "The channel is private and requires an invitation to join.");
    public static final Problem EmptyAccessType = new Problem("empty-access-type", "Empty Access Type", HttpStatus.BAD_REQUEST, "The access type is empty.");
    public static final Problem EmptyChannelName = new Problem("empty-channel-name", "Empty Channel Name", HttpStatus.BAD_REQUEST, "The channel name is empty.");
    public static final Problem InvalidChannelNameLength = new Problem("invalid-channel-name-length", "Invalid Channel Name Length", HttpStatus.BAD_REQUEST, "The channel name must be between 1 and 30 characters.");
    public static final Problem EmptyMessage = new Problem("empty-message", "Empty Message", HttpStatus.BAD_REQUEST, "The message is empty.");
    public static final Problem InvalidMessageLength = new Problem("invalid-message-length", "Invalid Message Length", HttpStatus.BAD_REQUEST, "The message content must be between 1 and 1000 characters.");
    public static final Problem EmptyPassword = new Problem("empty-password", "Empty Password", HttpStatus.BAD_REQUEST, "The password is empty.");
    public static final Problem EmptyToken = new Problem("empty-token", "Empty Token", HttpStatus.BAD_REQUEST, "The token is empty.");
    public static final Problem EmptyUsername = new Problem("empty-username", "Empty Username", HttpStatus.BAD_REQUEST, "The username is empty.");
    public static final Problem InvalidUsernameLength = new Problem("invalid-username-length", "Invalid Username Length", HttpStatus.BAD_REQUEST, "The username must be between 1 and 30 characters.");
    public static final Problem IncorrectPassword = new Problem("incorrect-password", "Incorrect Password", HttpStatus.UNAUTHORIZED, "The password is incorrect.");
    public static final Problem InsecurePassword = new Problem("insecure-password", "Insecure Password", HttpStatus.BAD_REQUEST, "The password is insecure.");
    public static final Problem InvalidAction = new Problem("invalid-action", "Invalid Action", HttpStatus.BAD_REQUEST, "The action is invalid.");
    public static final Problem InvalidExpirationTime = new Problem("invalid-expiration-time", "Invalid Expiration Time", HttpStatus.BAD_REQUEST, "The expiration time is invalid.");
    public static final Problem InvalidLimit = new Problem("invalid-limit", "Invalid Limit", HttpStatus.BAD_REQUEST, "The limit is invalid.");
    public static final Problem InvalidOffset = new Problem("invalid-offset", "Invalid Offset", HttpStatus.BAD_REQUEST, "The offset is invalid.");
    public static final Problem InvalidRequestContent = new Problem("invalid-request-content", "Invalid Request Content", HttpStatus.BAD_REQUEST, "The request content is invalid.");
    public static final Problem InvitationAlreadyExists = new Problem("invitation-already-exists", "Invitation Already Exists", HttpStatus.CONFLICT, "The invitation already exists.");
    public static final Problem InvitationAlreadyUsed = new Problem("invitation-already-used", "Invitation Already Used", HttpStatus.BAD_REQUEST, "The invitation has already been used.");
    public static final Problem InvitationExpired = new Problem("invitation-expired", "Invitation Expired", HttpStatus.BAD_REQUEST, "The invitation has expired.");
    public static final Problem InvitationNotFound = new Problem("invitation-not-found", "Invitation Not Found", HttpStatus.NOT_FOUND, "The invitation was not found.");
    public static final Problem MessagesNotFound = new Problem("messages-not-found", "Messages Not Found", HttpStatus.NOT_FOUND, "The messages were not found.");
    public static final Problem NoJoinedChannels = new Problem("no-joined-channels", "No Joined Channels", HttpStatus.NOT_FOUND, "The user has not joined any channel.");
    public static final Problem NoMatchingChannels = new Problem("no-matching-channels", "No Matching Channels", HttpStatus.NOT_FOUND, "The channel name does not match any channel.");
    public static final Problem OwnerCannotLeave = new Problem("owner-cannot-leave", "Owner Cannot Leave", HttpStatus.FORBIDDEN, "The owner cannot leave the channel.");
    public static final Problem PasswordSameAsPrevious = new Problem("password-same-as-previous", "Password Same As Previous", HttpStatus.BAD_REQUEST, "The password is the same as the previous one.");
    public static final Problem TokenNotFound = new Problem("token-not-found", "Token Not Found", HttpStatus.NOT_FOUND, "The token was not found.");
    public static final Problem UserAlreadyInChannel = new Problem("user-already-in-channel", "User Already In Channel", HttpStatus.FORBIDDEN, "The user is already in the channel.");
    public static final Problem UserIsOwner = new Problem("user-is-owner", "User Is Owner", HttpStatus.BAD_REQUEST, "The user is the owner of the channel.");
    public static final Problem UserHasOwnedChannels = new Problem("user-has-owned-channels", "User Has Owned Channels", HttpStatus.BAD_REQUEST, "The user has owned channels.");
    public static final Problem UserNotAuthorized = new Problem("user-not-authorized", "User Not Authorized", HttpStatus.UNAUTHORIZED, "The user is not authorized.");
    public static final Problem UserNotFound = new Problem("user-not-found", "User Not Found", HttpStatus.NOT_FOUND, "The user was not found.");
    public static final Problem UserNotInChannel = new Problem("user-not-in-channel", "User Not In Channel", HttpStatus.FORBIDDEN, "The user is not in the channel.");
    public static final Problem UserNotOwner = new Problem("user-not-owner", "User Not Owner", HttpStatus.BAD_REQUEST, "The user is not the owner of the channel.");
    public static final Problem UsernameAlreadyInUse = new Problem("username-already-in-use", "Username Already In Use", HttpStatus.CONFLICT, "The username is already in use.");
    public static final Problem RateLimitExceeded = new Problem("rate-limit-exceeded", "Too Many Requests", HttpStatus.TOO_MANY_REQUESTS, "You have exceeded the allowed number of requests. Please try again later.");
    public static final Problem InternalServerError = new Problem("internal-server-error", "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred.");
    private static final String MEDIA_TYPE = "application/problem+json";
    private static final String PROBLEM_URI_PATH = "https://github.com/isel-leic-daw/2024-daw-leic53d-g02-53d/tree/main/docs/instantMessaging";
    public final URI type;
    public final String title;
    public final HttpStatus status;
    public final String detail;

    private Problem(String path, String title, HttpStatus status, String detail) {
        this.type = URI.create(PROBLEM_URI_PATH + "/" + path);
        this.title = title;
        this.status = status;
        this.detail = detail;
    }

    public ResponseEntity<ProblemResponse> toResponseEntity() {
        return ResponseEntity
                .status(status)
                .header("Content-Type", MEDIA_TYPE)
                .body(new ProblemResponse(type.toString(), title, status.value(), detail));
    }
}