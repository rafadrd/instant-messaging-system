package pt.isel.api.users

data class RegisterInput(
    val username: String,
    val password: String,
    val invitationToken: String? = null,
)
