package pt.isel

import java.time.LocalDateTime

data class Message(
    val id: Long,
    val content: String,
    val user: UserInfo,
    val channel: Channel,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
