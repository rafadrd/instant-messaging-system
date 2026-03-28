dependencies {
    // Internal Modules
    implementation(project(":service"))

    // Spring
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)

    // Swagger UI
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    // Testing
    testImplementation(testFixtures(project(":domain")))
    testImplementation(project(":repository-jdbi"))
    testImplementation(project(":http-pipeline"))
    testImplementation(libs.jdbi3.core)
    testImplementation(libs.postgresql)
    testImplementation(libs.flyway.core)
    testImplementation(libs.flyway.database.postgresql)
}
