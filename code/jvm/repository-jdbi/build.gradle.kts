dependencies {
    // Internal Modules
    api(project(":repository"))

    // JDBI and Database
    implementation(libs.jdbi3.core)
    implementation(libs.jdbi3.postgres)
    implementation(libs.postgresql)

    testImplementation(libs.flyway.core)
    testImplementation(libs.flyway.database.postgresql)

    // Testcontainers
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)

    testImplementation(testFixtures(project(":repository")))
}
