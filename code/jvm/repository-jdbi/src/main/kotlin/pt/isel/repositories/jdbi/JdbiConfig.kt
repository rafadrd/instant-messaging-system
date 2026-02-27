package pt.isel.repositories.jdbi

import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.postgres.PostgresPlugin
import pt.isel.domain.security.PasswordValidationInfo
import pt.isel.repositories.jdbi.mapper.InstantMapper
import pt.isel.repositories.jdbi.mapper.PasswordValidationInfoMapper
import java.time.Instant

fun Jdbi.configureWithAppRequirements(): Jdbi {
    installPlugin(KotlinPlugin())
    installPlugin(PostgresPlugin())
    registerColumnMapper(PasswordValidationInfo::class.java, PasswordValidationInfoMapper())
    registerColumnMapper(Instant::class.java, InstantMapper())
    return this
}
