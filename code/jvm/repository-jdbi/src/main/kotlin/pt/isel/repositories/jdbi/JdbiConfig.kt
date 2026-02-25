package pt.isel.repositories.jdbi

import kotlinx.datetime.Instant
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.postgres.PostgresPlugin
import pt.isel.domain.auth.PasswordValidationInfo
import pt.isel.repositories.jdbi.mapper.InstantMapper
import pt.isel.repositories.jdbi.mapper.PasswordValidationInfoMapper

fun Jdbi.configureWithAppRequirements(): Jdbi {
    installPlugin(KotlinPlugin())
    installPlugin(PostgresPlugin())
    registerColumnMapper(PasswordValidationInfo::class.java, PasswordValidationInfoMapper())
    registerColumnMapper(Instant::class.java, InstantMapper())
    return this
}
