package pt.isel.domain.auth

import pt.isel.domain.User

class AuthenticatedUser(
    val user: User,
    val token: String,
)
