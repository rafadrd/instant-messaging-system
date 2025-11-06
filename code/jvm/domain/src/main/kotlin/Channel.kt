package pt.isel

data class Channel(
    val id: Long,
    val name: String,
    val owner: UserInfo,
    val isPublic: Boolean = true,
)
