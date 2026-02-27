package pt.isel.api.users

import com.fasterxml.jackson.annotation.JsonInclude

data class UserHomeOutput(
    val id: Long,
    val username: String,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val token: String? = null,
)
