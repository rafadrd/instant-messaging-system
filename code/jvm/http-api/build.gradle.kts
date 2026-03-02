dependencies {
    // Internal Modules
    implementation(project(":service"))

    // Spring
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)

    // Testing
    testImplementation(project(":repository-jdbi"))
    testImplementation(libs.jdbi3.core)
    testImplementation(libs.postgresql)
}
