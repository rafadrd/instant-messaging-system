package pt.isel.api.channels

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChannelInput(
    @field:NotBlank
    @field:Size(min = 1, max = 30)
    val name: String,
    val isPublic: Boolean,
)
