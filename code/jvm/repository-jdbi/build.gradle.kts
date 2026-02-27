dependencies {
    // Internal Modules
    api(project(":repository"))

    // JDBI
    implementation(libs.jdbi3.core)
    implementation(libs.jdbi3.kotlin)
    implementation(libs.jdbi3.postgres)

    // Database & Utilities
    implementation(libs.postgresql)
    implementation(libs.kotlin.reflect)
}
