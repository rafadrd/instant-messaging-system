dependencies {
    // Internal Modules
    api(project(":domain"))
    api(project(":repository"))

    // Libraries
    implementation(libs.jakarta.inject.api)

    // Testing
    testImplementation(testFixtures(project(":domain")))
    testImplementation(testFixtures(project(":repository")))
    testImplementation(project(":repository-jdbi"))
    testImplementation(libs.jdbi3.core)
    testImplementation(libs.postgresql)
    testImplementation(libs.flyway.core)
    testImplementation(libs.flyway.database.postgresql)
}
