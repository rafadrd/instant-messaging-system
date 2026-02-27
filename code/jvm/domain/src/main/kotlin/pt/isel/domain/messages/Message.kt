package pt.isel.domain.messages

import pt.isel.domain.channels.Channel
import pt.isel.domain.users.UserInfo
import java.time.LocalDateTime

data class Message(
    val id: Long,
    val content: String,
    val user: UserInfo,
    val channel: Channel,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
