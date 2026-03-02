package pt.isel.api.users

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterInput(
    @field:NotBlank
    @field:Size(min = 1, max = 30)
    val username: String,
    @field:NotBlank
    val password: String,
    val invitationToken: String? = null,
)
