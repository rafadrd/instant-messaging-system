plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
}

dependencies {
    // Internal Modules
    implementation(project(":domain"))
    implementation(project(":repository-jdbi"))
    implementation(project(":infrastructure"))
    implementation(project(":http-api"))
    implementation(project(":http-pipeline"))

    // Spring Boot
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.security.core)

    // Database
    implementation(libs.jdbi3.core)
    implementation(libs.postgresql)

    // Utilities
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)

    // Testing
    testImplementation(libs.spring.boot.starter.webflux)
}
