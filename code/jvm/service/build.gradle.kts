plugins { alias(libs.plugins.test.logger) }

dependencies {
    // Module dependencies
    api(project(":repository"))
    api(project(":domain"))

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
