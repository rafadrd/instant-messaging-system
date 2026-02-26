dependencies {
    // Module dependencies
    implementation(project(":repository"))
    implementation(project(":domain"))

    // JDBI and Postgres dependencies
    implementation(libs.jdbi3.core)
    implementation(libs.jdbi3.kotlin)
    implementation(libs.jdbi3.postgres)
    implementation(libs.postgresql)

    // Kotlin dependencies
    implementation(libs.kotlin.reflect)
}
