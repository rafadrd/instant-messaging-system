package pt.isel.api

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pt.isel.api.model.PageInput
import pt.isel.api.model.RegisterInput
import pt.isel.api.model.UpdateUsernameInput
import pt.isel.api.model.UserHomeOutput
import pt.isel.api.model.UserInput
import pt.isel.domain.auth.AuthenticatedUser
import pt.isel.services.ChannelService
import pt.isel.services.UserService

@RestController
@RequestMapping("/api")
class UserController(
    private val userService: UserService,
    private val channelService: ChannelService,
) {
    @PostMapping("/auth/register")
    fun registerUser(
        @RequestBody user: RegisterInput,
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
        @RequestBody user: UserInput,
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
        @RequestBody input: UpdateUsernameInput,
    ): ResponseEntity<*> = handleResult(userService.updateUsername(user.user.id, input.newUsername, input.password))

    @GetMapping("/users/me/channels")
    fun getUserChannels(
        user: AuthenticatedUser,
        @RequestParam page: PageInput = PageInput(),
    ): ResponseEntity<*> = handleResult(channelService.getJoinedChannels(user.user.id, page.limit, page.offset))

    @DeleteMapping("/users/me")
    fun deleteUser(user: AuthenticatedUser): ResponseEntity<*> = handleResult(userService.deleteUser(user.user.id))
}
