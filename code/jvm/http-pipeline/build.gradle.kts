dependencies {
    // Internal Modules
    implementation(project(":service"))

    // Spring
    implementation(libs.spring.boot.starter.web)

    // Testing
    testImplementation(testFixtures(project(":domain")))
}
