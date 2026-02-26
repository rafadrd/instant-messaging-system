plugins {
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    // Internal Module Dependencies
    implementation(project(":domain"))
    implementation(project(":service"))
    implementation(project(":repository"))

    // Spring Security
    api(libs.spring.security.core)

    // Redis Implementation
    implementation(libs.spring.boot.starter.data.redis)

    // JWT Implementation
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    // JSON Handling
    implementation(libs.jackson.module.kotlin)
}
