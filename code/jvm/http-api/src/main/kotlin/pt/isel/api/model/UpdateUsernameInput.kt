package pt.isel.api.model

data class UpdateUsernameInput(
    val newUsername: String,
    val password: String,
)
