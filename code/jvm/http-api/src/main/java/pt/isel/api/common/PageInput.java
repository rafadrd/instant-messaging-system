package pt.isel.api.common;

public record PageInput(Integer limit, Integer offset) {
    public PageInput {
        if (limit == null) limit = 50;
        if (offset == null) offset = 0;
    }
}