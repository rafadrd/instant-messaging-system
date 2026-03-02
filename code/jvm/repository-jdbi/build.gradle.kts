dependencies {
    // Internal Modules
    api(project(":repository"))

    // JDBI and Database
    implementation(libs.jdbi3.core)
    implementation(libs.jdbi3.postgres)
    implementation(libs.postgresql)
}
