package pt.isel.api.common;

import java.beans.ConstructorProperties;

public record PageInput(Integer limit, Integer offset) {

    @ConstructorProperties({"limit", "offset"})
    public PageInput(Integer limit, Integer offset) {
        this.limit = (limit == null || limit <= 0) ? 50 : Math.min(limit, 100);
        this.offset = (offset == null || offset < 0) ? 0 : offset;
    }
}