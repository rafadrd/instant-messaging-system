package pt.isel.repositories.jdbi.utils

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet

private class ResultSetMapper<T>(
    private val mapper: (ResultSet) -> T,
) : RowMapper<T> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): T = mapper(rs)
}

internal fun Handle.executeUpdate(
    query: String,
    params: Map<String, Any?> = emptyMap(),
): Int =
    createUpdate(query.trimIndent())
        .apply { params.forEach { (key, value) -> bind(key, value) } }
        .execute()

internal fun Handle.executeUpdateAndReturnId(
    query: String,
    params: Map<String, Any?> = emptyMap(),
): Long =
    createUpdate(query.trimIndent())
        .apply { params.forEach { (key, value) -> bind(key, value) } }
        .executeAndReturnGeneratedKeys()
        .mapTo(Long::class.java)
        .one()

internal fun <T> Handle.executeQueryToList(
    query: String,
    params: Map<String, Any?> = emptyMap(),
    mapper: (rs: ResultSet) -> T,
): List<T> =
    createQuery(query.trimIndent())
        .apply { params.forEach { (key, value) -> bind(key, value) } }
        .map(ResultSetMapper(mapper))
        .list()

internal fun <T> Handle.executeQueryToSingle(
    query: String,
    params: Map<String, Any?> = emptyMap(),
    mapper: (rs: ResultSet) -> T,
): T? =
    createQuery(query.trimIndent())
        .apply { params.forEach { (key, value) -> bind(key, value) } }
        .map(ResultSetMapper(mapper))
        .findOne()
        .orElse(null)
