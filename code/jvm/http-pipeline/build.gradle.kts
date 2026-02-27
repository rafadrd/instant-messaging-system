plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.dependency.management)
}

dependencies {
    // Module dependencies
    implementation(project(":service"))
    implementation(project(":domain"))

    // To use Spring MVC and the Servlet API
    implementation(libs.spring.webmvc)
    implementation(libs.jakarta.servlet.api)

    // Spring Boot dependencies
    implementation(libs.spring.boot.starter)
}
