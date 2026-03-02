package pt.isel.api.messages

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class MessageRequest(
    @field:NotBlank
    @field:Size(min = 1, max = 1000)
    val content: String,
)
