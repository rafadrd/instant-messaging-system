package pt.isel.api.users;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.isel.api.common.ErrorHandling;
import pt.isel.api.common.PageInput;
import pt.isel.domain.users.AuthenticatedUser;
import pt.isel.services.channels.ChannelService;
import pt.isel.services.users.UserService;

@RestController
@RequestMapping("/api")
@Tag(name = "Users", description = "Users and Authentication management")
public class UserController {
    private final UserService userService;
    private final ChannelService channelService;

    public UserController(UserService userService, ChannelService channelService) {
        this.userService = userService;
        this.channelService = channelService;
    }

    @PostMapping("/auth/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterInput user) {
        return ErrorHandling.handleResult(
                userService.registerUser(user.username(), user.password(), user.invitationToken()),
                tokenInfo -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(new UserHomeOutput(tokenInfo.userId(), user.username(), tokenInfo.tokenValue()))
        );
    }

    @PostMapping("/auth/login")
    @Operation(summary = "Login a user")
    public ResponseEntity<?> loginUser(@Valid @RequestBody UserInput user) {
        return ErrorHandling.handleResult(
                userService.createToken(user.username(), user.password()),
                tokenInfo -> ResponseEntity.ok(new UserHomeOutput(tokenInfo.userId(), user.username(), tokenInfo.tokenValue()))
        );
    }

    @PostMapping("/auth/logout")
    @Operation(summary = "Logout the user")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<Void> logout(@Parameter(hidden = true) AuthenticatedUser user) {
        userService.revokeToken(user.token());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/me")
    @Operation(summary = "Get current user information")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<UserHomeOutput> userHome(@Parameter(hidden = true) AuthenticatedUser user) {
        return ResponseEntity.ok(new UserHomeOutput(user.user().id(), user.user().username(), null));
    }

    @PutMapping("/users/me")
    @Operation(summary = "Update username")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<?> editUser(@Parameter(hidden = true) AuthenticatedUser user, @Valid @RequestBody UpdateUsernameInput input) {
        return ErrorHandling.handleResult(
                userService.updateUsername(user.user().id(), input.newUsername(), input.password()),
                updatedUser -> ResponseEntity.ok(new UserHomeOutput(updatedUser.id(), updatedUser.username(), null))
        );
    }

    @PutMapping("/users/me/password")
    @Operation(summary = "Update password")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<?> updatePassword(@Parameter(hidden = true) AuthenticatedUser user, @Valid @RequestBody UpdatePasswordInput input) {
        return ErrorHandling.handleResult(
                userService.updatePassword(user.user().id(), input.oldPassword(), input.newPassword()),
                updatedUser -> ResponseEntity.ok(new UserHomeOutput(updatedUser.id(), updatedUser.username(), null))
        );
    }

    @GetMapping("/users/me/channels")
    @Operation(summary = "Get channels for the current user")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<?> getUserChannels(@Parameter(hidden = true) AuthenticatedUser user, PageInput page) {
        return ErrorHandling.handleResult(channelService.getJoinedChannels(user.user().id(), page.limit(), page.offset()));
    }

    @DeleteMapping("/users/me")
    @Operation(summary = "Delete user account")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<?> deleteUser(@Parameter(hidden = true) AuthenticatedUser user) {
        return ErrorHandling.handleResult(userService.deleteUser(user.user().id()));
    }
}