plugins {
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    // Module dependencies
    implementation(project(":service"))

    // To use Spring MVC and the Servlet API
    implementation(libs.spring.webmvc)
    implementation(libs.jakarta.servlet.api)

    // Spring Boot dependencies
    implementation(libs.spring.boot.starter)

    // To use SLF4J
    implementation(libs.slf4j.api)
}
