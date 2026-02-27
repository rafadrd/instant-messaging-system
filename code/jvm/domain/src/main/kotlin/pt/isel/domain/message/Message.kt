package pt.isel.domain.message

import pt.isel.domain.channel.Channel
import pt.isel.domain.user.UserInfo
import java.time.LocalDateTime

data class Message(
    val id: Long,
    val content: String,
    val user: UserInfo,
    val channel: Channel,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
