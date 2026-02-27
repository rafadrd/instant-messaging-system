import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.ktlint) apply false
}

allprojects {
    group = "pt.isel"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

// Environment & Docker Configuration
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

// Subprojects Configuration
subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java-library")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<KotlinProjectExtension> {
        jvmToolchain(21)
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
        }
    }

    dependencies {
        add("api", platform(rootProject.libs.spring.boot.dependencies))
        add("testImplementation", rootProject.libs.spring.boot.starter.test)
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
