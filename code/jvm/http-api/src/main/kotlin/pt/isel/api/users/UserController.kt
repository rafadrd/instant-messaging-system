package pt.isel.api.users

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pt.isel.api.common.PageInput
import pt.isel.api.common.handleResult
import pt.isel.domain.users.AuthenticatedUser
import pt.isel.services.channels.ChannelService
import pt.isel.services.users.UserService

@RestController
@RequestMapping("/api")
class UserController(
    private val userService: UserService,
    private val channelService: ChannelService,
) {
    @PostMapping("/auth/register")
    fun registerUser(
        @Valid @RequestBody user: RegisterInput,
    ): ResponseEntity<*> =
        handleResult(
            userService.registerUser(user.username, user.password, user.invitationToken),
        ) { tokenInfo ->
            ResponseEntity
                .status(HttpStatus.CREATED)
                .body(UserHomeOutput(tokenInfo.userId, user.username, tokenInfo.tokenValue))
        }

    @PostMapping("/auth/login")
    fun loginUser(
        @Valid @RequestBody user: UserInput,
    ): ResponseEntity<*> =
        handleResult(userService.createToken(user.username, user.password)) { tokenInfo ->
            ResponseEntity.ok(UserHomeOutput(tokenInfo.userId, user.username, tokenInfo.tokenValue))
        }

    @PostMapping("/auth/logout")
    fun logout(user: AuthenticatedUser): ResponseEntity<Unit> {
        userService.revokeToken(user.token)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/users/me")
    fun userHome(user: AuthenticatedUser): ResponseEntity<UserHomeOutput> =
        ResponseEntity.ok(UserHomeOutput(user.user.id, user.user.username, null))

    @PutMapping("/users/me")
    fun editUser(
        user: AuthenticatedUser,
        @Valid @RequestBody input: UpdateUsernameInput,
    ): ResponseEntity<*> =
        handleResult(userService.updateUsername(user.user.id, input.newUsername, input.password)) { updatedUser ->
            ResponseEntity.ok(UserHomeOutput(updatedUser.id, updatedUser.username))
        }

    @PutMapping("/users/me/password")
    fun updatePassword(
        user: AuthenticatedUser,
        @Valid @RequestBody input: UpdatePasswordInput,
    ): ResponseEntity<*> =
        handleResult(userService.updatePassword(user.user.id, input.oldPassword, input.newPassword)) { updatedUser ->
            ResponseEntity.ok(UserHomeOutput(updatedUser.id, updatedUser.username))
        }

    @GetMapping("/users/me/channels")
    fun getUserChannels(
        user: AuthenticatedUser,
        page: PageInput = PageInput(),
    ): ResponseEntity<*> = handleResult(channelService.getJoinedChannels(user.user.id, page.limit, page.offset))

    @DeleteMapping("/users/me")
    fun deleteUser(user: AuthenticatedUser): ResponseEntity<*> = handleResult(userService.deleteUser(user.user.id))
}
