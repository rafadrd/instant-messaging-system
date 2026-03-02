dependencies {
    // Internal Modules
    api(project(":domain"))
    api(project(":repository"))

    // Libraries
    implementation(libs.jakarta.inject.api)

    // Testing
    testImplementation(project(":repository-jdbi"))
    testImplementation(libs.jdbi3.core)
    testImplementation(libs.postgresql)
}
