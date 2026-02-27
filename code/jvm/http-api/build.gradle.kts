plugins {
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    // Internal Modules
    implementation(project(":service"))

    // Spring
    implementation(libs.spring.boot.starter.web)

    // Testing
    testImplementation(project(":repository-jdbi"))
    testImplementation(libs.jdbi3.core)
    testImplementation(libs.postgresql)
    testImplementation(libs.spring.boot.starter.webflux)
}
