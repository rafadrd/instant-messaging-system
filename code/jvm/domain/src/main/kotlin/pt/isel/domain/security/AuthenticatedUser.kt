package pt.isel.domain.security

import pt.isel.domain.user.User

class AuthenticatedUser(
    val user: User,
    val token: String,
)
