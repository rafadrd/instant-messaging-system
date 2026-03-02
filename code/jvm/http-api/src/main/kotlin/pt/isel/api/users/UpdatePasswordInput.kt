package pt.isel.api.users

import jakarta.validation.constraints.NotBlank

data class UpdatePasswordInput(
    @field:NotBlank
    val oldPassword: String,
    @field:NotBlank
    val newPassword: String,
)
