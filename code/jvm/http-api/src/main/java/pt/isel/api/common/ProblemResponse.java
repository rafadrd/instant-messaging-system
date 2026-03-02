package pt.isel.api.common;

public record ProblemResponse(
        String type,
        String title,
        int status,
        String detail
) {
}