dependencies {
    // Internal Modules
    implementation(project(":service"))

    // Spring
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)

    // Swagger UI
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    // Testing
    testImplementation(project(":repository-jdbi"))
    testImplementation(libs.jdbi3.core)
    testImplementation(libs.postgresql)
    testImplementation(libs.flyway.core)
    testImplementation(libs.flyway.database.postgresql)
    testImplementation(testFixtures(project(":domain")))
}
