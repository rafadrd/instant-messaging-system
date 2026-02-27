plugins {
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    // Internal Modules
    implementation(project(":service"))
    implementation(project(":repository"))

    // Spring
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.security.core)

    // Utilities / JWT
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jjwt.api)

    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
}
