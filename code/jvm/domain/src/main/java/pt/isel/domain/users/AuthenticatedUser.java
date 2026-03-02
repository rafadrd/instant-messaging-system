package pt.isel.domain.users;

public record AuthenticatedUser(
        User user,
        String token
) {
}