plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.dependency.management)
}

dependencies {
    // Internal Module dependencies
    implementation(project(":http-api"))
    implementation(project(":repository-jdbi"))
    implementation(project(":http-pipeline"))
    implementation(project(":infrastructure"))

    // Spring Boot dependencies
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.web)

    // Kotlin dependencies
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)

    // JDBI and Postgres dependencies
    implementation(libs.jdbi3.core)
    implementation(libs.postgresql)

    // DotEnv
    implementation(libs.dotenv.kotlin)

    // Test dependencies
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webflux)
}
