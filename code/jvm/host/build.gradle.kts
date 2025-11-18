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

    // Test dependencies
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webflux)
}

tasks.withType<Test> {
    environment("DB_URL", "jdbc:postgresql://localhost:5432/db?user=dbuser&password=isel")
    dependsOn(":repository-jdbi:dbTestsWait")
    finalizedBy(":repository-jdbi:dbTestsDown")
}
