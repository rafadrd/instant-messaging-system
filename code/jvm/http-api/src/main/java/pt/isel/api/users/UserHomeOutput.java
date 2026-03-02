package pt.isel.api.users;

import com.fasterxml.jackson.annotation.JsonInclude;

public record UserHomeOutput(
        Long id,
        String username,
        @JsonInclude(JsonInclude.Include.NON_NULL) String token
) {
}