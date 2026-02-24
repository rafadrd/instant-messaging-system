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

    // To use Kotlin specific date and time functions
    implementation(libs.kotlinx.datetime)

    // Redis for Pub/Sub
    implementation(libs.spring.boot.starter.data.redis)

    // Jackson for JSON serialization/deserialization in Redis messages
    implementation(libs.jackson.module.kotlin)

    // JDBI and Postgres dependencies
    testImplementation(project(":repository-jdbi"))
    testImplementation(libs.jdbi3.core)
    testImplementation(libs.postgresql)

    // Test dependencies
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.withType<Test> {
    environment("DB_URL", "jdbc:postgresql://localhost:5432/db?user=dbuser&password=isel")
    dependsOn(":repository-jdbi:dbTestsWait")
    finalizedBy(":repository-jdbi:dbTestsDown")
}
