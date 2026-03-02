package pt.isel.repositories.jdbi.utils;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import pt.isel.domain.security.PasswordValidationInfo;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PasswordValidationInfoMapper implements ColumnMapper<PasswordValidationInfo> {
    @Override
    public PasswordValidationInfo map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
        return new PasswordValidationInfo(r.getString(columnNumber));
    }
}