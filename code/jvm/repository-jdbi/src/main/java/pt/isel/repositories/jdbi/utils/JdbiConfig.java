package pt.isel.repositories.jdbi.utils;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;
import pt.isel.domain.security.PasswordValidationInfo;

public class JdbiConfig {
    public static Jdbi configureWithAppRequirements(Jdbi jdbi) {
        jdbi.installPlugin(new PostgresPlugin());
        jdbi.registerColumnMapper(PasswordValidationInfo.class, new PasswordValidationInfoMapper());
        return jdbi;
    }
}