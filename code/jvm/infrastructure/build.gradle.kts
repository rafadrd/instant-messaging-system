plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.dependency.management)
}

dependencies {
    // Internal Module Dependencies
    implementation(project(":domain"))
    implementation(project(":service"))
    implementation(project(":repository"))

    // Spring Boot dependencies
    implementation(libs.spring.boot.starter)

    // Redis Implementation
    implementation(libs.spring.boot.starter.data.redis)

    // JWT Implementation
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    // JSON Handling
    implementation(libs.jackson.module.kotlin)

    // Utils
    implementation(libs.kotlinx.datetime)
}
