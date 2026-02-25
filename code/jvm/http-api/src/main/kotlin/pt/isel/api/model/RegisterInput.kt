package pt.isel.api.model

data class RegisterInput(
    val username: String,
    val password: String,
    val invitationToken: String? = null,
)
