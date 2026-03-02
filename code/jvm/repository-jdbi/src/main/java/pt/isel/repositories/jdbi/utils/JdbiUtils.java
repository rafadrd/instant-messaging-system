package pt.isel.repositories.jdbi.utils;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JdbiUtils {

    private static <T> RowMapper<T> asRowMapper(ResultSetMapper<T> mapper) {
        return (rs, ctx) -> mapper.map(rs);
    }

    public static Map<String, Object> params(Object... kvs) {
        if (kvs.length % 2 != 0) throw new IllegalArgumentException("Key-value array must have even length");
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < kvs.length; i += 2) {
            map.put((String) kvs[i], kvs[i + 1]);
        }
        return map;
    }

    public static void executeUpdate(Handle handle, String query, Map<String, Object> params) {
        var update = handle.createUpdate(query);
        params.forEach(update::bind);
        update.execute();
    }

    public static Long executeUpdateAndReturnId(Handle handle, String query, Map<String, Object> params) {
        var update = handle.createUpdate(query);
        params.forEach(update::bind);
        return update.executeAndReturnGeneratedKeys()
                .mapTo(Long.class)
                .one();
    }

    public static <T> List<T> executeQueryToList(Handle handle, String query, Map<String, Object> params, ResultSetMapper<T> mapper) {
        var q = handle.createQuery(query);
        params.forEach(q::bind);
        return q.map(asRowMapper(mapper)).list();
    }

    public static <T> T executeQueryToSingle(Handle handle, String query, Map<String, Object> params, ResultSetMapper<T> mapper) {
        var q = handle.createQuery(query);
        params.forEach(q::bind);
        return q.map(asRowMapper(mapper)).findOne().orElse(null);
    }

    @FunctionalInterface
    public interface ResultSetMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }
}