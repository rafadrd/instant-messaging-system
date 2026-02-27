dependencies {
    // Module dependencies
    implementation(project(":repository"))
    implementation(project(":domain"))

    // To get the DI annotation
    implementation(libs.jakarta.inject.api)
    implementation(libs.jakarta.annotation.api)

    // To use SLF4J
    implementation(libs.slf4j.api)

    // JDBI and Postgres dependencies
    testImplementation(project(":repository-jdbi"))
    testImplementation(libs.jdbi3.core)
    testImplementation(libs.postgresql)
}
