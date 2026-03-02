package pt.isel.api.messages

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pt.isel.api.common.PageInput
import pt.isel.api.common.handleResult
import pt.isel.domain.users.AuthenticatedUser
import pt.isel.services.messages.MessageService

@RestController
@RequestMapping("/api/channels/{channelId}/messages")
class MessageController(
    private val messageService: MessageService,
) {
    @PostMapping
    fun createMessage(
        user: AuthenticatedUser,
        @PathVariable channelId: Long,
        @Valid @RequestBody request: MessageRequest,
    ): ResponseEntity<*> =
        handleResult(
            messageService.createMessage(
                content = request.content,
                userId = user.user.id,
                channelId = channelId,
            ),
        ) { message ->
            ResponseEntity.status(HttpStatus.CREATED).body(message)
        }

    @GetMapping
    fun getMessages(
        user: AuthenticatedUser,
        @PathVariable channelId: Long,
        page: PageInput = PageInput(),
    ): ResponseEntity<*> =
        handleResult(
            messageService.getMessagesInChannel(
                userId = user.user.id,
                channelId = channelId,
                limit = page.limit,
                offset = page.offset,
            ),
        )
}
