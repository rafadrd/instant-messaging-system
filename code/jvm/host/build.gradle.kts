plugins {
    java
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
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)

    // Swagger UI
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    // Testing
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(testFixtures(project(":domain")))
    testImplementation(testFixtures(project(":repository")))
}
