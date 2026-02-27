plugins {
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    // Internal Modules
    implementation(project(":service"))

    // Spring
    implementation(libs.spring.boot.starter.web)
}
