plugins { alias(libs.plugins.test.logger) }

dependencies {
    // Module dependencies
    api(project(":service"))

    // To use Spring MVC and the Servlet API
    implementation(libs.spring.webmvc)
    implementation(libs.jakarta.servlet.api)

    // To use SLF4J
    implementation(libs.slf4j.api)

    // for JDBI and Postgres Tests
    testImplementation(libs.jdbi3.core)
    testImplementation(libs.postgresql)
    testImplementation(project(":repository-jdbi"))

    // To use WebTestClient on tests
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

kotlin { compilerOptions { freeCompilerArgs.addAll("-Xjsr305=strict") } }

tasks.withType<Test> {
    environment("DB_URL", "jdbc:postgresql://localhost:5432/db?user=dbuser&password=isel")
    dependsOn(":repository-jdbi:dbTestsWait")
    finalizedBy(":repository-jdbi:dbTestsDown")
}
