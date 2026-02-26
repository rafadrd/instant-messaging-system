dependencies {
    // Module dependencies
    api(project(":service"))

    // To use Spring MVC and the Servlet API
    implementation(libs.spring.webmvc)
    implementation(libs.jakarta.servlet.api)

    // To use SLF4J
    implementation(libs.slf4j.api)

    // For @JsonInclude annotations in DTOs
    implementation(libs.jackson.module.kotlin)

    // for JDBI and Postgres Tests
    testImplementation(libs.jdbi3.core)
    testImplementation(libs.postgresql)
    testImplementation(project(":repository-jdbi"))

    // WebFlux starter
    testImplementation(libs.spring.boot.starter.webflux)
}

kotlin { compilerOptions { freeCompilerArgs.addAll("-Xjsr305=strict") } }
