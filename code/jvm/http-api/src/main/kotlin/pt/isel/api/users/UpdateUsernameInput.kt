package pt.isel.api.users

data class UpdateUsernameInput(
    val newUsername: String,
    val password: String,
)
