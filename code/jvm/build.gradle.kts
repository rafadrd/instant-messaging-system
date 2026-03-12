import java.util.*

plugins {
    java
    alias(libs.plugins.spring.boot) apply false
}

allprojects {
    group = "pt.isel"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

// Environment and docker configuration
val envFile = file("../../.env")
val dockerComposePath: String = file("../../docker-compose.yml").absolutePath

val envVars =
    if (envFile.exists()) {
        Properties()
            .apply { envFile.inputStream().use { load(it) } }
            .entries
            .associate { it.key.toString() to it.value.toString() }
    } else {
        emptyMap()
    }

val dbTestsUp by tasks.registering(Exec::class) {
    group = "database"
    description = "Starts the PostgreSQL container"
    commandLine("docker", "compose", "-f", dockerComposePath, "up", "-d", "postgres")
}

val dbTestsWait by tasks.registering(Exec::class) {
    group = "database"
    description = "Waits for PostgreSQL to be ready"
    dependsOn(dbTestsUp)
    commandLine("docker", "exec", "ims-postgres", "sh", "-c", "until pg_isready -U dbuser -d db; do sleep 1; done")
}

val dbTestsDown by tasks.registering(Exec::class) {
    group = "database"
    description = "Stops the PostgreSQL container"
    commandLine("docker", "compose", "-f", dockerComposePath, "stop", "postgres")
}

// Subprojects configuration
subprojects {
    apply(plugin = "java-library")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    dependencies {
        add("api", platform(rootProject.libs.spring.boot.dependencies))
        add("testImplementation", rootProject.libs.spring.boot.starter.test)
        add("testRuntimeOnly", rootProject.libs.junit.platform.launcher)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()

        environment(envVars)

        val dbDependentModules = setOf("repository-jdbi", "service", "http-api", "host", "infrastructure")
        if (project.name in dbDependentModules) {
            dependsOn(dbTestsWait)
        }
    }
}
