package pt.isel.api.common;

public record PageInput(Integer limit, Integer offset) {
    public PageInput {
        if (limit == null || limit <= 0) limit = 50;
        if (limit > 100) limit = 100;
        if (offset == null || offset < 0) offset = 0;
    }
}