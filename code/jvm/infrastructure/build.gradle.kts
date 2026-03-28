dependencies {
    // Internal Modules
    implementation(project(":service"))
    implementation(project(":repository"))

    // Spring
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.security.core)

    // Jackson
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)

    // JWT
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    // Testing
    testImplementation(testFixtures(project(":repository")))
    testImplementation(testFixtures(project(":domain")))
}
