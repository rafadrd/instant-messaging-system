package pt.isel.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import pt.isel.api.model.ChannelInput
import pt.isel.api.model.EditChannelInput
import pt.isel.api.model.EditMemberInput
import pt.isel.api.model.JoinByTokenInput
import pt.isel.api.model.PageInput
import pt.isel.domain.security.AuthenticatedUser
import pt.isel.services.ChannelService
import pt.isel.services.MessageEventService

@RestController
@RequestMapping("/api/channels")
class ChannelController(
    private val channelService: ChannelService,
    private val messageEventService: MessageEventService,
) {
    @GetMapping
    fun getChannels(
        user: AuthenticatedUser,
        @RequestParam query: String = "",
        @RequestParam page: PageInput = PageInput(),
    ): ResponseEntity<*> = handleResult(channelService.searchChannels(query, page.limit, page.offset))

    @PostMapping
    fun createChannel(
        user: AuthenticatedUser,
        @RequestBody channel: ChannelInput,
    ): ResponseEntity<*> =
        handleResult(
            channelService.createChannel(
                name = channel.name,
                ownerId = user.user.id,
                isPublic = channel.isPublic,
            ),
        )

    @GetMapping("/{channelId}")
    fun getChannelById(
        user: AuthenticatedUser,
        @PathVariable channelId: Long,
    ): ResponseEntity<*> = handleResult(channelService.getChannelById(channelId))

    @PutMapping("/{channelId}")
    fun editChannel(
        user: AuthenticatedUser,
        @PathVariable channelId: Long,
        @RequestBody input: EditChannelInput,
    ): ResponseEntity<*> =
        handleResult(
            channelService.editChannel(
                ownerId = user.user.id,
                channelId = channelId,
                name = input.name,
                isPublic = input.isPublic,
            ),
        )

    @DeleteMapping("/{channelId}")
    fun deleteChannel(
        user: AuthenticatedUser,
        @PathVariable channelId: Long,
    ): ResponseEntity<*> = handleResult(channelService.deleteChannel(ownerId = user.user.id, channelId = channelId))

    @PostMapping("/{channelId}/join")
    fun joinChannel(
        user: AuthenticatedUser,
        @PathVariable channelId: Long,
    ): ResponseEntity<*> = handleResult(channelService.joinPublicChannel(userId = user.user.id, channelId = channelId))

    @PostMapping("/join-by-token")
    fun joinChannelByToken(
        user: AuthenticatedUser,
        @RequestBody input: JoinByTokenInput,
    ): ResponseEntity<*> = handleResult(channelService.joinPrivateChannel(userId = user.user.id, token = input.token))

    @PostMapping("/{channelId}/leave")
    fun leaveChannel(
        user: AuthenticatedUser,
        @PathVariable channelId: Long,
    ): ResponseEntity<*> = handleResult(channelService.leaveChannel(channelId, user.user.id))

    @GetMapping("/{channelId}/members")
    fun getMembers(
        user: AuthenticatedUser,
        @PathVariable channelId: Long,
        @RequestParam page: PageInput = PageInput(),
    ): ResponseEntity<*> = handleResult(channelService.getUsersInChannel(channelId, page.limit, page.offset))

    @GetMapping("/{channelId}/members/{userId}")
    fun getAccessType(
        @PathVariable userId: Long,
        @PathVariable channelId: Long,
    ): ResponseEntity<*> = handleResult(channelService.getAccessType(userId, channelId))

    @PutMapping("/{channelId}/members/{userId}")
    fun editMemberAccess(
        user: AuthenticatedUser,
        @PathVariable channelId: Long,
        @PathVariable userId: Long,
        @RequestBody input: EditMemberInput,
    ): ResponseEntity<*> = handleResult(channelService.editMemberAccess(user.user.id, channelId, userId, input.accessType))

    @GetMapping("/{channelId}/listen")
    fun listen(
        user: AuthenticatedUser,
        @PathVariable channelId: Long,
    ): ResponseEntity<*> =
        handleResult(channelService.getAccessType(user.user.id, channelId)) {
            val emitter = SseEmitter(0L)
            try {
                val adapter = SseUpdatedMessageEmitterAdapter(emitter)
                messageEventService.addEmitter(channelId, user.user.id, adapter)
            } catch (e: Exception) {
                emitter.completeWithError(e)
            }
            ResponseEntity.ok(emitter)
        }
}
