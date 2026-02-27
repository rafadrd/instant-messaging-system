package pt.isel.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pt.isel.api.model.MessageRequest
import pt.isel.api.model.PageInput
import pt.isel.domain.security.AuthenticatedUser
import pt.isel.services.MessageService

@RestController
@RequestMapping("/api/channels/{channelId}/messages")
class MessageController(
    private val messageService: MessageService,
) {
    @PostMapping
    fun createMessage(
        user: AuthenticatedUser,
        @PathVariable channelId: Long,
        @RequestBody request: MessageRequest,
    ): ResponseEntity<*> =
        handleResult(
            messageService.createMessage(
                content = request.content,
                userId = user.user.id,
                channelId = channelId,
            ),
        )

    @GetMapping
    fun getMessages(
        user: AuthenticatedUser,
        @PathVariable channelId: Long,
        @RequestParam page: PageInput = PageInput(),
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
