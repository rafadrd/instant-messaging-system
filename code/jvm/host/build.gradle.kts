import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.dependency.management)
}

dependencies {
    // Module dependencies
    implementation(project(":http-api"))
    implementation(project(":repository-jdbi"))
    implementation(project(":http-pipeline"))

    // Spring Boot dependencies
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.web)

    // Kotlin dependencies
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.datetime)

    // JDBI and Postgres dependencies
    implementation(libs.jdbi3.core)
    implementation(libs.postgresql)

    // DotEnv
    implementation(libs.dotenv.kotlin)

    // Redis
    implementation(libs.spring.boot.starter.data.redis)

    // Test dependencies
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webflux)
}

fun loadEnv(): Map<String, String> {
    val envFile =
        rootProject.projectDir.parentFile
            ?.parentFile
            ?.resolve(".env")

    if (envFile == null || !envFile.exists()) {
        return emptyMap()
    }

    val props = Properties()
    props.load(FileInputStream(envFile))
    return props.entries.associate { it.key.toString() to it.value.toString() }
}

tasks.withType<Test> {
    val envVars = loadEnv()
    environment(envVars)

    if (!envVars.containsKey("DB_URL")) {
        environment("DB_URL", "jdbc:postgresql://localhost:5432/db?user=dbuser&password=isel")
    }

    dependsOn(":repository-jdbi:dbTestsWait")
    finalizedBy(":repository-jdbi:dbTestsDown")
}
