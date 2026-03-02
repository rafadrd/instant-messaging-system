package pt.isel.api.users;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.isel.api.common.ErrorHandling;
import pt.isel.domain.users.AuthenticatedUser;
import pt.isel.services.channels.ChannelService;
import pt.isel.services.users.UserService;

@RestController
@RequestMapping("/api")
public class UserController {
    private final UserService userService;
    private final ChannelService channelService;

    public UserController(UserService userService, ChannelService channelService) {
        this.userService = userService;
        this.channelService = channelService;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterInput user) {
        return ErrorHandling.handleResult(
                userService.registerUser(user.username(), user.password(), user.invitationToken()),
                tokenInfo -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(new UserHomeOutput(tokenInfo.userId(), user.username(), tokenInfo.tokenValue()))
        );
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody UserInput user) {
        return ErrorHandling.handleResult(
                userService.createToken(user.username(), user.password()),
                tokenInfo -> ResponseEntity.ok(new UserHomeOutput(tokenInfo.userId(), user.username(), tokenInfo.tokenValue()))
        );
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(AuthenticatedUser user) {
        userService.revokeToken(user.token());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/me")
    public ResponseEntity<UserHomeOutput> userHome(AuthenticatedUser user) {
        return ResponseEntity.ok(new UserHomeOutput(user.user().id(), user.user().username(), null));
    }

    @PutMapping("/users/me")
    public ResponseEntity<?> editUser(AuthenticatedUser user, @Valid @RequestBody UpdateUsernameInput input) {
        return ErrorHandling.handleResult(
                userService.updateUsername(user.user().id(), input.newUsername(), input.password()),
                updatedUser -> ResponseEntity.ok(new UserHomeOutput(updatedUser.id(), updatedUser.username(), null))
        );
    }

    @PutMapping("/users/me/password")
    public ResponseEntity<?> updatePassword(AuthenticatedUser user, @Valid @RequestBody UpdatePasswordInput input) {
        return ErrorHandling.handleResult(
                userService.updatePassword(user.user().id(), input.oldPassword(), input.newPassword()),
                updatedUser -> ResponseEntity.ok(new UserHomeOutput(updatedUser.id(), updatedUser.username(), null))
        );
    }

    @GetMapping("/users/me/channels")
    public ResponseEntity<?> getUserChannels(AuthenticatedUser user,
                                             @RequestParam(defaultValue = "50") int limit,
                                             @RequestParam(defaultValue = "0") int offset) {
        return ErrorHandling.handleResult(channelService.getJoinedChannels(user.user().id(), limit, offset));
    }

    @DeleteMapping("/users/me")
    public ResponseEntity<?> deleteUser(AuthenticatedUser user) {
        return ErrorHandling.handleResult(userService.deleteUser(user.user().id()));
    }
}